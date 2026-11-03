package gr.ntua.medialab.energyisland.model;

/**
 * An existing load such as a village or hotel ({@code "type": "LOAD"}).
 * Demand = {@code scale x profile(category, hour) x 1000} kW.
 *
 * @param id       unique id
 * @param position cell (settlement only)
 * @param category load category; must be a column of every day CSV
 * @param scale    scaling factor of the normalised profile
 */
public record Load(String id, Position position, String category, double scale) implements Unit {

    /** Base power that a normalised profile value of 1.0 corresponds to, at scale 1.0 (kW). */
    public static final double BASE_KW = 1000.0;

    /**
     * Computes this load's demand for one hour.
     *
     * @param profileValue normalised profile value of the load's category for that hour
     * @return demand in kW (equal to kWh for the one-hour step)
     */
    public double demandKw(double profileValue) {
        return scale * profileValue * BASE_KW;
    }

    @Override
    public Load withPosition(Position newPosition) {
        return new Load(id, newPosition, category, scale);
    }
}
