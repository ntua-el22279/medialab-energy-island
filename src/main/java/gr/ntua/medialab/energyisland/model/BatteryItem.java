package gr.ntua.medialab.energyisland.model;

/**
 * A battery. Charging with energy E from the grid raises the stored energy by
 * {@code E x efficiency}; discharging E to the grid lowers it by E. Per hour the
 * battery either charges or discharges, by at most {@code pmax}.
 *
 * @param id         catalog id
 * @param size       capacity, kWh
 * @param capex      investment, EUR
 * @param life       lifetime, years
 * @param pmax       maximum charge/discharge power, kW
 * @param efficiency charging efficiency, 0-1 ({@code eff})
 * @param socMin     minimum state of charge, fraction of capacity ({@code soc_min})
 * @param socMax     maximum state of charge, fraction of capacity ({@code soc_max})
 */
public record BatteryItem(String id, double size, double capex, double life,
                          double pmax, double efficiency, double socMin, double socMax)
        implements CatalogItem {

    /** Nominal state of charge at the start of every simulated day (before clamping). */
    public static final double INITIAL_SOC = 0.5;

    @Override
    public TechType type() {
        return TechType.BATTERY;
    }

    /**
     * Returns the lowest allowed stored energy.
     *
     * @return {@code socMin x size}, kWh
     */
    public double minEnergy() {
        return socMin * size;
    }

    /**
     * Returns the highest allowed stored energy.
     *
     * @return {@code socMax x size}, kWh
     */
    public double maxEnergy() {
        return socMax * size;
    }

    /**
     * Returns the stored energy at the start of each day: 50 % of capacity, clamped to
     * the nearest limit when 50 % lies outside {@code [socMin, socMax]} (brief v2).
     *
     * @return {@code min(max(0.5, socMin), socMax) x size}, kWh
     */
    public double initialEnergy() {
        return Math.min(Math.max(INITIAL_SOC, socMin), socMax) * size;
    }
}
