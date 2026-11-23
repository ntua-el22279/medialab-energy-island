package gr.ntua.medialab.energyisland.simulation;

/**
 * What the controller decided for one hour. All values are energies for that hour, in kWh
 * (equal to the average power in kW over the hour), and none is negative.
 *
 * @param rule      the rule that applied
 * @param diesel    diesel output {@code P_D}
 * @param discharge total energy given by the batteries {@code P_B^dis}
 * @param charge    total energy taken by the batteries {@code P_B^ch}
 * @param unserved  demand that was not covered {@code U} (blackout)
 * @param curtailed energy from renewables or diesel minimum that was thrown away {@code C}
 */
public record Dispatch(ControlRule rule, double diesel, double discharge, double charge,
                       double unserved, double curtailed) {

    /**
     * Returns whether the diesel plant runs this hour.
     *
     * @return {@code true} if {@code diesel > 0}
     */
    public boolean dieselOn() {
        return diesel > 0;
    }
}
