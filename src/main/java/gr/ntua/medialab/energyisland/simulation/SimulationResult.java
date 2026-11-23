package gr.ntua.medialab.energyisland.simulation;

import gr.ntua.medialab.energyisland.model.Plan;

import java.util.List;

/**
 * The outcome of simulating a plan over all representative days: per-day detail and the
 * annual figures of the brief (A.1, "Σχέδια και κόστος").
 * <p>
 * Annual cost {@code K = Σ I_i/T_i + Σ_d w_d (fuel·E_D,d + start·S_d + λ·co2·E_D,d)}.
 * A plan is <em>accepted</em> when no day has a blackout and its investment is within budget.
 */
public final class SimulationResult {

    private final Plan plan;
    private final List<String> batterySiteIds;
    private final List<DayResult> days;
    private final double investment;
    private final double annualizedInvestment;
    private final double budget;
    private final double annualOperatingCost;
    private final double annualEmissions;
    private final double annualLoad;
    private final double annualDiesel;
    private final double annualUnserved;
    private final double annualCurtailed;

    /**
     * Builds the result and its annual totals.
     *
     * @param plan                 the simulated plan
     * @param batterySiteIds       sites holding batteries, in the order of {@link HourResult#batteryLevels()}
     * @param days                 the result of each representative day
     * @param investment           total investment {@code Σ I_i}, EUR
     * @param annualizedInvestment {@code Σ I_i / T_i}, EUR per year
     * @param budget               the island's investment budget, EUR
     */
    public SimulationResult(Plan plan, List<String> batterySiteIds, List<DayResult> days,
                            double investment, double annualizedInvestment, double budget) {
        this.plan = plan.copy();
        this.batterySiteIds = List.copyOf(batterySiteIds);
        this.days = List.copyOf(days);
        this.investment = investment;
        this.annualizedInvestment = annualizedInvestment;
        this.budget = budget;
        double cost = 0, em = 0, l = 0, d = 0, u = 0, c = 0;
        for (DayResult day : this.days) {
            int w = day.weight();
            cost += w * day.operatingCost();
            em += w * day.emissions();
            l += w * day.load();
            d += w * day.diesel();
            u += w * day.unserved();
            c += w * day.curtailed();
        }
        this.annualOperatingCost = cost;
        this.annualEmissions = em;
        this.annualLoad = l;
        this.annualDiesel = d;
        this.annualUnserved = u;
        this.annualCurtailed = c;
    }

    /** @return a copy of the simulated plan */
    public Plan plan() {
        return plan.copy();
    }

    /** @return sites holding batteries, in the order of {@link HourResult#batteryLevels()} */
    public List<String> batterySiteIds() {
        return batterySiteIds;
    }

    /** @return the result of each representative day, in scenario order */
    public List<DayResult> days() {
        return days;
    }

    /** @return total investment {@code Σ I_i}, EUR */
    public double investment() {
        return investment;
    }

    /** @return annualised investment {@code Σ I_i / T_i}, EUR per year */
    public double annualizedInvestment() {
        return annualizedInvestment;
    }

    /** @return the island's investment budget, EUR */
    public double budget() {
        return budget;
    }

    /** @return {@code Σ_d w_d (fuel·E + start·S + λ·co2·E)}, EUR per year */
    public double annualOperatingCost() {
        return annualOperatingCost;
    }

    /** @return annual cost {@code K}, EUR per year */
    public double annualCost() {
        return annualizedInvestment + annualOperatingCost;
    }

    /** @return weighted annual diesel emissions, kg CO2 */
    public double annualEmissions() {
        return annualEmissions;
    }

    /** @return weighted annual demand {@code Σ w_d L_d}, kWh */
    public double annualLoad() {
        return annualLoad;
    }

    /** @return weighted annual diesel energy {@code Σ w_d E_D,d}, kWh */
    public double annualDiesel() {
        return annualDiesel;
    }

    /** @return weighted annual unserved energy, kWh */
    public double annualUnserved() {
        return annualUnserved;
    }

    /** @return weighted annual curtailed energy, kWh */
    public double annualCurtailed() {
        return annualCurtailed;
    }

    /**
     * Returns the renewable share {@code 1 - Σ w_d E_D,d / Σ w_d L_d}. It is informational
     * only and does not affect acceptance.
     *
     * @return share between 0 and 1; 1 if there is no demand
     */
    public double renewableShare() {
        return annualLoad > 0 ? 1 - annualDiesel / annualLoad : 1.0;
    }

    /** @return whether at least one day has unserved demand */
    public boolean hasBlackout() {
        return days.stream().anyMatch(DayResult::hasBlackout);
    }

    /** @return whether the investment does not exceed the budget */
    public boolean withinBudget() {
        return investment <= budget;
    }

    /** @return whether the plan is accepted: no blackout on any day and within budget */
    public boolean isAccepted() {
        return withinBudget() && !hasBlackout();
    }
}
