package gr.ntua.medialab.energyisland.simulation;

import gr.ntua.medialab.energyisland.model.BatteryItem;
import gr.ntua.medialab.energyisland.model.Catalog;
import gr.ntua.medialab.energyisland.model.CatalogItem;
import gr.ntua.medialab.energyisland.model.DayProfile;
import gr.ntua.medialab.energyisland.model.DieselPlant;
import gr.ntua.medialab.energyisland.model.Island;
import gr.ntua.medialab.energyisland.model.Load;
import gr.ntua.medialab.energyisland.model.Plan;
import gr.ntua.medialab.energyisland.model.PvItem;
import gr.ntua.medialab.energyisland.model.Scenario;
import gr.ntua.medialab.energyisland.model.Site;
import gr.ntua.medialab.energyisland.model.Terrain;
import gr.ntua.medialab.energyisland.model.WindItem;
import gr.ntua.medialab.energyisland.validation.PlanValidator;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates plans hour by hour over the representative days of a scenario.
 * <p>
 * Each day is independent: batteries start at {@link BatteryItem#initialEnergy()} and the
 * diesel plant starts off, so running in hour 0 counts as a start. Every hour the demand and
 * the renewable production are computed and the {@link OperatingController} decides the rest.
 * <p>
 * The simulator takes a snapshot of the island when it is created (loads, diesel, sites,
 * terrain), so later edits to the island do not affect it; create a new one after editing.
 * It holds no state between calls and can be shared by several threads.
 */
public final class Simulator {

    private final Island island;
    private final Catalog catalog;
    private final List<DayProfile> days;
    private final DieselPlant diesel;
    private final OperatingController controller;
    /** Hourly demand per day, kWh: {@code loads[day][hour]}. */
    private final double[][] loads;

    /**
     * Creates a simulator for a scenario.
     *
     * @param scenario a valid scenario (exactly one diesel plant, every load category in every day)
     * @throws IllegalArgumentException if the island has no diesel plant
     */
    public Simulator(Scenario scenario) {
        this.island = scenario.island().copy();
        this.catalog = scenario.catalog();
        this.days = scenario.days();
        this.diesel = island.diesel().orElseThrow(
                () -> new IllegalArgumentException("The island has no diesel plant"));
        this.controller = new OperatingController(diesel);
        this.loads = new double[days.size()][DayProfile.HOURS];
        List<Load> islandLoads = island.loads();
        for (int d = 0; d < days.size(); d++) {
            DayProfile day = days.get(d);
            for (int h = 0; h < DayProfile.HOURS; h++) {
                double sum = 0;
                for (Load load : islandLoads) {
                    sum += load.demandKw(day.loadProfile(load.category(), h));
                }
                loads[d][h] = sum;
            }
        }
    }

    /**
     * Returns the representative days in the order they are simulated.
     *
     * @return the days of the scenario
     */
    public List<DayProfile> days() {
        return days;
    }

    /**
     * Simulates a plan over all representative days.
     *
     * @param plan the plan; an empty plan means "build nothing"
     * @return per-hour, per-day and annual results
     * @throws InvalidPlanException if the plan refers to unknown sites or items, or puts an
     *                              item where its technology is not allowed
     */
    public SimulationResult simulate(Plan plan) {
        List<ValidationIssue> issues = PlanValidator.validate(plan, island, catalog);
        if (!issues.isEmpty()) {
            throw new InvalidPlanException(issues);
        }
        Installation inst = new Installation(plan);
        List<DayResult> results = new ArrayList<>(days.size());
        for (int d = 0; d < days.size(); d++) {
            results.add(simulateDay(inst, d));
        }
        List<String> batterySites = inst.batteries.stream().map(BatteryState::siteId).toList();
        return new SimulationResult(plan, batterySites, results,
                plan.investment(catalog), plan.annualizedInvestment(catalog), island.getBudget());
    }

    /** Runs the 24 hours of day {@code d}. */
    private DayResult simulateDay(Installation inst, int d) {
        DayProfile day = days.get(d);
        for (BatteryState b : inst.batteries) {
            b.reset();
        }
        List<HourResult> hours = new ArrayList<>(DayProfile.HOURS);
        boolean wasOn = false;
        for (int h = 0; h < DayProfile.HOURS; h++) {
            double load = loads[d][h];
            double pv = inst.pvOutput(day.solar(h));
            double wind = inst.windOutput(day.windSpeed(h));
            Dispatch dispatch = controller.dispatch(load - (pv + wind), inst.batteries);
            boolean on = dispatch.dieselOn();
            double[] levels = new double[inst.batteries.size()];
            for (int i = 0; i < levels.length; i++) {
                levels[i] = inst.batteries.get(i).level();
            }
            hours.add(new HourResult(h, load, pv, wind, dispatch, on && !wasOn, levels));
            wasOn = on;
        }
        return new DayResult(day.name(), day.weight(), hours, diesel);
    }

    /** The plan's items resolved against the island's sites, in site order. */
    private final class Installation {
        private final List<PvItem> pv = new ArrayList<>();
        private final List<WindItem> wind = new ArrayList<>();
        private final List<Terrain> windTerrain = new ArrayList<>();
        private final List<BatteryState> batteries = new ArrayList<>();

        Installation(Plan plan) {
            for (Site site : island.sites()) {
                String itemId = plan.itemAt(site.id()).orElse(null);
                if (itemId == null) {
                    continue;
                }
                CatalogItem item = catalog.find(itemId).orElseThrow();
                switch (item) {
                    case PvItem p -> pv.add(p);
                    case WindItem w -> {
                        wind.add(w);
                        windTerrain.add(island.terrainAt(site.position()));
                    }
                    case BatteryItem b -> batteries.add(new BatteryState(site.id(), b));
                }
            }
        }

        double pvOutput(double solar) {
            double sum = 0;
            for (PvItem p : pv) {
                sum += p.outputKw(solar);
            }
            return sum;
        }

        double windOutput(double groundSpeed) {
            double sum = 0;
            for (int i = 0; i < wind.size(); i++) {
                sum += wind.get(i).outputKw(WindItem.effectiveSpeed(groundSpeed, windTerrain.get(i)));
            }
            return sum;
        }
    }
}
