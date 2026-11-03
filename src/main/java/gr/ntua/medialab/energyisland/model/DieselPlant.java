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

    @Override
    public DieselPlant withPosition(Position newPosition) {
        return new DieselPlant(id, newPosition, pmin, pmax, fuelCost, co2, startCost);
    }
}
