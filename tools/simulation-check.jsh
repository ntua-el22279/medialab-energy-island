// Simulation regression check.
// Run with: tools/check-simulation.sh   (from the project root)
// 1. Controller cases computed by hand (guide §6 + brief v2 rules 1-4).
// 2. Invariants on every hour of sample scenarios a and b, for several plans.
// 3. Empty plan: annual cost checked against a closed-form formula.

import gr.ntua.medialab.energyisland.io.*;
import gr.ntua.medialab.energyisland.model.*;
import gr.ntua.medialab.energyisland.simulation.*;
import java.nio.file.*;
import java.util.*;

int passed = 0, failed = 0;
double EPS = 1e-9;

void check(String name, boolean ok, String detail) {
    if (ok) { passed++; System.out.println("PASS " + name); }
    else { failed++; System.out.println("FAIL " + name + "  " + detail); }
}
boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

// ---------- 1. controller by hand ----------
// diesel pmin 100, pmax 700; battery 300 kWh, pmax 150, eff 0.9, soc 0.1..0.95 -> 30..285 kWh, start 150
var DSL = new DieselPlant("d", new Position(0, 0), 100, 700, 0.3, 0.75, 30);
var B300 = new BatteryItem("b300", 300, 120000, 12, 150, 0.9, 0.1, 0.95);
var ctl = new OperatingController(DSL);

void ctlCase(String name, double n, double startLevel, ControlRule rule, double pd, double dis,
             double ch, double u, double c, double endLevel) {
    var b = new BatteryState("s", B300);
    while (b.level() > startLevel + EPS) b.discharge(b.level() - startLevel);
    while (b.level() < startLevel - EPS) b.charge((startLevel - b.level()) / B300.efficiency());
    var x = ctl.dispatch(n, List.of(b));
    boolean ok = x.rule() == rule && near(x.diesel(), pd) && near(x.discharge(), dis) && near(x.charge(), ch)
            && near(x.unserved(), u) && near(x.curtailed(), c) && near(b.level(), endLevel);
    check("controller " + name, ok, x + " level=" + b.level());
}
ctlCase("R1 surplus fits",         -100, 150, ControlRule.SURPLUS,   0,   0, 100,   0,  0, 240);
ctlCase("R1 surplus > pmax/room",  -200, 150, ControlRule.SURPLUS,   0,   0, 150,   0, 50, 285);
ctlCase("R1 battery full",          -40, 285, ControlRule.SURPLUS,   0,   0,   0,   0, 40, 285);
ctlCase("R2 battery covers",         80, 150, ControlRule.BATTERIES, 0,  80,   0,   0,  0,  70);
ctlCase("R2 exactly D_max",         120, 150, ControlRule.BATTERIES, 0, 120,   0,   0,  0,  30);
ctlCase("R3 N-Dmax<pmin, discharge less", 200, 150, ControlRule.DIESEL, 100, 100, 0, 0, 0, 50);
ctlCase("R3 empty battery, pmin surplus charges", 50, 30, ControlRule.DIESEL, 100, 0, 50, 0, 0, 75);
{   // full battery with pmax 10: D_max = 10 < N = 50 -> P_D = 100, rest -50 cannot be stored -> curtailed
    var small = new BatteryItem("b", 300, 0, 12, 10, 0.9, 0.1, 0.5);   // starts at 150 = soc_max
    var b = new BatteryState("s", small);
    var x = ctl.dispatch(50, List.of(b));
    check("controller R3 pmin surplus > charge room", x.rule() == ControlRule.DIESEL && near(x.diesel(), 100)
            && near(x.charge(), 0) && near(x.curtailed(), 50) && near(b.level(), 150), x + " level=" + b.level());
}
ctlCase("R3 N-Dmax above pmin",     500, 150, ControlRule.DIESEL,  380, 120,  0,   0,  0,  30);
ctlCase("R4 blackout at pmax",     1000, 150, ControlRule.DIESEL,  700, 120,  0, 180,  0,  30);
{   // no batteries at all
    var x = ctl.dispatch(30, List.of());
    check("controller no battery, pmin > N", x.rule() == ControlRule.DIESEL && near(x.diesel(), 100)
            && near(x.curtailed(), 70), x.toString());
}
{   // two batteries are used in order: first one as much as it can, the rest to the second
    var b1 = new BatteryState("s1", B300); var b2 = new BatteryState("s2", B300);
    var x = ctl.dispatch(200, List.of(b1, b2));  // D_max = 240
    check("controller two batteries in order", x.rule() == ControlRule.BATTERIES && near(b1.level(), 30)
            && near(b2.level(), 70), "levels " + b1.level() + " " + b2.level());
    var c1 = new BatteryState("s1", B300); var c2 = new BatteryState("s2", B300);
    ctl.dispatch(-160, List.of(c1, c2));
    check("controller two batteries charge in order", near(c1.level(), 285) && near(c2.level(), 159),
            "levels " + c1.level() + " " + c2.level());
}

// ---------- 2. invariants on the samples ----------
var loader = new ScenarioLoader();
void invariants(String label, SimulationResult r, Scenario sc) {
    var bats = r.batterySiteIds().stream()
            .map(id -> (BatteryItem) sc.catalog().find(r.plan().itemAt(id).get()).get()).toList();
    int bad = 0; String first = "";
    for (var day : r.days()) {
        boolean wasOn = false; int starts = 0;
        for (var h : day.hours()) {
            var d = h.dispatch();
            List<String> errs = new ArrayList<>();
            if (Math.abs(h.balanceError()) > 1e-6) errs.add("balance " + h.balanceError());
            if (d.charge() > EPS && d.discharge() > EPS) errs.add("charge+discharge");
            if (d.unserved() > 1e-6 && d.curtailed() > 1e-6) errs.add("unserved+curtailed");
            var plant = sc.island().diesel().get();
            if (d.diesel() > EPS && (d.diesel() < plant.pmin() - EPS || d.diesel() > plant.pmax() + EPS))
                errs.add("diesel outside pmin..pmax");
            double[] lv = h.batteryLevels();
            for (int i = 0; i < lv.length; i++) {
                if (lv[i] < bats.get(i).minEnergy() - EPS || lv[i] > bats.get(i).maxEnergy() + EPS) errs.add("level " + lv[i]);
            }
            if (h.dieselStart() != (d.dieselOn() && !wasOn)) errs.add("start flag");
            if (h.dieselStart()) starts++;
            wasOn = d.dieselOn();
            if (!errs.isEmpty()) { bad++; if (first.isEmpty()) first = day.dayName() + " h" + h.hour() + " " + errs; }
        }
        if (starts != day.starts()) { bad++; first = day.dayName() + " start count"; }
    }
    check("invariants " + label, bad == 0, bad + " bad hours, first: " + first);
    System.out.printf("     K=%,.2f  invest=%,.0f/%,.0f  annualised=%,.2f  opex=%,.2f  RES=%.1f%%  CO2=%,.0f kg  unserved=%,.1f  curtailed=%,.1f  accepted=%s%n",
            r.annualCost(), r.investment(), r.budget(), r.annualizedInvestment(), r.annualOperatingCost(),
            100 * r.renewableShare(), r.annualEmissions(), r.annualUnserved(), r.annualCurtailed(), r.isAccepted());
}

Plan planOf(String sc, String... pairs) {
    var p = new Plan(sc);
    for (int i = 0; i < pairs.length; i += 2) p.assign(pairs[i], pairs[i + 1]);
    return p;
}

var A = loader.load(Path.of("medialab/scenarios/a"));
var B = loader.load(Path.of("medialab/scenarios/b"));
var simA = new Simulator(A);
var simB = new Simulator(B);

invariants("a empty", simA.simulate(new Plan("a")), A);
invariants("a mixed", simA.simulate(planOf("a", "s1", "w150", "s3", "b300", "s4", "pv200", "s5", "b300")), A);
invariants("a all-in", simA.simulate(planOf("a", "s1", "w150", "s2", "w150", "s3", "pv200", "s4", "pv200", "s5", "b300", "s6", "w150")), A);
// scenario b: the first allowed item at every site, then the largest of each type
var pb1 = new Plan("b"); var pb2 = new Plan("b");
for (var s : B.island().sites()) {
    var items = B.catalog().itemsAllowedAt(s);
    pb1.assign(s.id(), items.get(0).id());
    pb2.assign(s.id(), items.stream().max(Comparator.comparingDouble(CatalogItem::size)).get().id());
}
invariants("b empty", simB.simulate(new Plan("b")), B);
invariants("b first-items", simB.simulate(pb1), B);
invariants("b largest-items", simB.simulate(pb2), B);

// ---------- 3. empty plan vs closed form ----------
// With nothing built: P_D = min(max(L, pmin), pmax) every hour (diesel runs whenever L > 0).
void closedForm(String label, Scenario sc, Simulator sim) {
    var dsl = sc.island().diesel().get();
    double K = 0;
    for (var day : sc.days()) {
        double e = 0; int starts = 0; boolean on = false;
        for (int h = 0; h < 24; h++) {
            double L = 0;
            for (var l : sc.island().loads()) L += l.demandKw(day.loadProfile(l.category(), h));
            double pd = L > 0 ? Math.min(Math.max(L, dsl.pmin()), dsl.pmax()) : 0;
            if (pd > 0 && !on) starts++;
            on = pd > 0; e += pd;
        }
        K += day.weight() * (dsl.fuelCost() * e + dsl.startCost() * starts + 0.08 * dsl.co2() * e);
    }
    double got = sim.simulate(new Plan(sc.id())).annualCost();
    check("empty plan closed form " + label, Math.abs(got - K) < 0.01, "sim " + got + " vs " + K);
}
closedForm("a", A, simA);
closedForm("b", B, simB);

// ---------- 4. invalid plans are rejected ----------
void expectInvalid(String name, Simulator sim, Plan p, String expected) {
    try { sim.simulate(p); check("invalid plan " + name, false, "was accepted"); }
    catch (InvalidPlanException e) { check("invalid plan " + name, e.getMessage().contains(expected), e.getMessage()); }
}
expectInvalid("unknown site", simA, planOf("a", "zz", "pv100"), "no candidate site");
expectInvalid("unknown item", simA, planOf("a", "s3", "pv999"), "unknown catalog item");
expectInvalid("type not allowed", simA, planOf("a", "s1", "pv100"), "not allowed");

System.out.println();
System.out.println(passed + " passed, " + failed + " failed");
/exit failed == 0 ? 0 : 1
