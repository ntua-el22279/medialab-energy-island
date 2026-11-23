package gr.ntua.medialab.energyisland.simulation;

import gr.ntua.medialab.energyisland.model.BatteryItem;

/**
 * The changing state of one installed battery during a simulated day (guide §6).
 * <p>
 * Charging with energy E taken from the grid raises the level by {@code E x eff}; discharging
 * E to the grid lowers it by E. The level stays in {@code [soc_min x size, soc_max x size]}
 * and each hour moves at most {@code pmax} through the grid connection.
 */
public final class BatteryState {

    private final String siteId;
    private final BatteryItem item;
    private double level;

    /**
     * Creates the battery at its start-of-day level ({@link BatteryItem#initialEnergy()}).
     *
     * @param siteId the site where the battery is installed
     * @param item   the catalog battery
     */
    public BatteryState(String siteId, BatteryItem item) {
        this.siteId = siteId;
        this.item = item;
        reset();
    }

    /** Puts the battery back to its start-of-day level. */
    public void reset() {
        level = item.initialEnergy();
    }

    /**
     * Returns the site where the battery is installed.
     *
     * @return the site id
     */
    public String siteId() {
        return siteId;
    }

    /**
     * Returns the catalog battery.
     *
     * @return the battery item
     */
    public BatteryItem item() {
        return item;
    }

    /**
     * Returns the stored energy.
     *
     * @return current level, kWh
     */
    public double level() {
        return level;
    }

    /**
     * Returns the most energy the battery can give to the grid this hour.
     *
     * @return {@code min(pmax, level - soc_min x size)}, kWh, never negative
     */
    public double maxDischarge() {
        return Math.max(0.0, Math.min(item.pmax(), level - item.minEnergy()));
    }

    /**
     * Returns the most energy the battery can take from the grid this hour.
     *
     * @return {@code min(pmax, (soc_max x size - level) / eff)}, kWh, never negative
     */
    public double maxCharge() {
        return Math.max(0.0, Math.min(item.pmax(), (item.maxEnergy() - level) / item.efficiency()));
    }

    /**
     * Discharges as much of the requested energy as the limits allow.
     *
     * @param requested energy wanted by the grid, kWh
     * @return energy actually given to the grid, kWh
     */
    public double discharge(double requested) {
        double e = Math.min(Math.max(0.0, requested), maxDischarge());
        level = clamp(level - e);
        return e;
    }

    /**
     * Charges with as much of the offered energy as the limits allow.
     *
     * @param offered energy available from the grid, kWh
     * @return energy actually taken from the grid, kWh
     */
    public double charge(double offered) {
        double e = Math.min(Math.max(0.0, offered), maxCharge());
        level = clamp(level + e * item.efficiency());
        return e;
    }

    /** Removes floating-point drift so the level never leaves its limits. */
    private double clamp(double value) {
        return Math.min(item.maxEnergy(), Math.max(item.minEnergy(), value));
    }
}
