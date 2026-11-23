package gr.ntua.medialab.energyisland.simulation;

import gr.ntua.medialab.energyisland.model.DieselPlant;

import java.util.List;

/**
 * The 24 hours of one representative day and their totals.
 * Energies are in kWh for the day, not yet multiplied by the day's weight.
 */
public final class DayResult {

    /** Unserved energy at or below this (kWh) counts as no blackout; it absorbs rounding only. */
    public static final double BLACKOUT_TOLERANCE = 1e-6;

    private final String dayName;
    private final int weight;
    private final List<HourResult> hours;
    private final double load;
    private final double pv;
    private final double wind;
    private final double diesel;
    private final double discharge;
    private final double charge;
    private final double unserved;
    private final double curtailed;
    private final int starts;
    private final double emissions;
    private final double operatingCost;

    /**
     * Builds the day result and its totals.
     *
     * @param dayName    name of the day file (e.g. {@code winter.csv})
     * @param weight     number of days of the year this day stands for
     * @param hours      the 24 hourly results
     * @param dieselPlant the diesel plant, for emissions and cost
     */
    public DayResult(String dayName, int weight, List<HourResult> hours, DieselPlant dieselPlant) {
        this.dayName = dayName;
        this.weight = weight;
        this.hours = List.copyOf(hours);
        double l = 0, p = 0, w = 0, d = 0, dis = 0, ch = 0, u = 0, c = 0;
        int s = 0;
        for (HourResult h : this.hours) {
            l += h.load();
            p += h.pv();
            w += h.wind();
            d += h.dispatch().diesel();
            dis += h.dispatch().discharge();
            ch += h.dispatch().charge();
            u += h.dispatch().unserved();
            c += h.dispatch().curtailed();
            if (h.dieselStart()) {
                s++;
            }
        }
        this.load = l;
        this.pv = p;
        this.wind = w;
        this.diesel = d;
        this.discharge = dis;
        this.charge = ch;
        this.unserved = u;
        this.curtailed = c;
        this.starts = s;
        this.emissions = dieselPlant.emissionsKg(d);
        this.operatingCost = dieselPlant.dailyCost(d, s);
    }

    /** @return name of the day file */
    public String dayName() {
        return dayName;
    }

    /** @return number of days of the year this day stands for ({@code w_d}) */
    public int weight() {
        return weight;
    }

    /** @return the 24 hourly results, in order */
    public List<HourResult> hours() {
        return hours;
    }

    /** @return total demand {@code L_d}, kWh */
    public double load() {
        return load;
    }

    /** @return PV energy produced, kWh */
    public double pv() {
        return pv;
    }

    /** @return wind energy produced, kWh */
    public double wind() {
        return wind;
    }

    /** @return diesel energy {@code E_D,d}, kWh */
    public double diesel() {
        return diesel;
    }

    /** @return energy given by the batteries, kWh */
    public double discharge() {
        return discharge;
    }

    /** @return energy taken by the batteries, kWh */
    public double charge() {
        return charge;
    }

    /** @return unserved demand, kWh */
    public double unserved() {
        return unserved;
    }

    /** @return curtailed energy, kWh */
    public double curtailed() {
        return curtailed;
    }

    /** @return number of diesel starts {@code S_d} */
    public int starts() {
        return starts;
    }

    /** @return diesel emissions {@code co2 x E_D,d}, kg CO2 */
    public double emissions() {
        return emissions;
    }

    /** @return operating cost {@code fuel·E + start·S + λ·co2·E} of one such day, EUR */
    public double operatingCost() {
        return operatingCost;
    }

    /** @return whether some demand was not covered during the day */
    public boolean hasBlackout() {
        return unserved > BLACKOUT_TOLERANCE;
    }
}
