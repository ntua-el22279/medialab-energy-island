package gr.ntua.medialab.energyisland.simulation;

/**
 * The energy flows of one simulated hour. Energies are in kWh for the hour (equal to the
 * average power in kW). The balance {@code pv + wind + diesel + discharge + unserved =
 * load + charge + curtailed} holds up to rounding.
 *
 * @param hour           hour of the day, 0-23
 * @param load           total demand {@code L}
 * @param pv             PV production {@code P_PV}
 * @param wind           wind production {@code P_W}
 * @param dispatch       the controller decision (diesel, batteries, unserved, curtailed)
 * @param dieselStart    whether the diesel plant started this hour
 * @param batteryLevels  stored energy of each battery at the end of the hour, kWh, in site order
 */
public record HourResult(int hour, double load, double pv, double wind, Dispatch dispatch,
                         boolean dieselStart, double[] batteryLevels) {

    /**
     * Copies the battery levels so the record cannot be changed from outside.
     */
    public HourResult {
        batteryLevels = batteryLevels.clone();
    }

    /**
     * Returns the stored energy of each battery at the end of the hour.
     *
     * @return a copy of the levels, kWh, in site order
     */
    @Override
    public double[] batteryLevels() {
        return batteryLevels.clone();
    }

    /**
     * Returns the total stored energy at the end of the hour.
     *
     * @return sum of all battery levels, kWh
     */
    public double storedEnergy() {
        double sum = 0;
        for (double l : batteryLevels) {
            sum += l;
        }
        return sum;
    }

    /**
     * Returns the net load {@code N = L - (P_PV + P_W)} the controller saw.
     *
     * @return net load, kWh; negative means surplus
     */
    public double netLoad() {
        return load - (pv + wind);
    }

    /**
     * Returns the energy balance left over: supply minus use. It should be about 0.
     *
     * @return {@code (pv + wind + diesel + discharge + unserved) - (load + charge + curtailed)}, kWh
     */
    public double balanceError() {
        Dispatch d = dispatch;
        return (pv + wind + d.diesel() + d.discharge() + d.unserved())
                - (load + d.charge() + d.curtailed());
    }
}
