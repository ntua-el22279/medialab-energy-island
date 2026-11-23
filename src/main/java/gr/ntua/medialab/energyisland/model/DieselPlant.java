package gr.ntua.medialab.energyisland.model;

/**
 * The existing diesel plant ({@code "type": "DIESEL"}). It is controllable ON/OFF and,
 * when running, produces between {@code pmin} and {@code pmax}.
 *
 * @param id        unique id
 * @param position  cell (land or mountain)
 * @param pmin      minimum power when running, kW
 * @param pmax      maximum power, kW
 * @param fuelCost  fuel cost, EUR per kWh produced ({@code fuel})
 * @param co2       emissions, kg CO2 per kWh produced ({@code co2})
 * @param startCost cost of each start, EUR ({@code start})
 */
public record DieselPlant(String id, Position position, double pmin, double pmax,
                          double fuelCost, double co2, double startCost) implements Unit {

    /**
     * Carbon price λ, EUR per kg CO2. Fixed and the same for every scenario (brief v2);
     * it is not read from the scenario files.
     */
    public static final double CARBON_PRICE = 0.08;

    /**
     * Returns the emissions for the given diesel energy.
     *
     * @param energyKwh energy produced by the plant, kWh
     * @return {@code co2 x energyKwh}, kg CO2
     */
    public double emissionsKg(double energyKwh) {
        return co2 * energyKwh;
    }

    /**
     * Returns the operating cost of one day: fuel, starts and emissions priced at
     * {@link #CARBON_PRICE}, that is {@code fuel·E + start·S + λ·co2·E}.
     *
     * @param energyKwh energy produced by the plant during the day, kWh
     * @param starts    number of starts during the day
     * @return daily operating cost, EUR
     */
    public double dailyCost(double energyKwh, int starts) {
        return fuelCost * energyKwh + startCost * starts + CARBON_PRICE * emissionsKg(energyKwh);
    }

    @Override
    public DieselPlant withPosition(Position newPosition) {
        return new DieselPlant(id, newPosition, pmin, pmax, fuelCost, co2, startCost);
    }
}
