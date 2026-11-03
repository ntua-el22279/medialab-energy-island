package gr.ntua.medialab.energyisland.model;

/**
 * A wind turbine with a cubic power curve between cut-in and rated speed.
 *
 * @param id     catalog id
 * @param size   rated power, kW
 * @param capex  investment, EUR
 * @param life   lifetime, years
 * @param cutIn  cut-in speed, m/s ({@code cutin})
 * @param rated  rated speed, m/s
 * @param cutOut cut-out speed, m/s ({@code cutout})
 */
public record WindItem(String id, double size, double capex, double life,
                       double cutIn, double rated, double cutOut) implements CatalogItem {

    /** Wind speed multiplier for turbines on mountain cells. */
    public static final double MOUNTAIN_FACTOR = 1.15;

    @Override
    public TechType type() {
        return TechType.WIND;
    }

    /**
     * Returns the wind speed a turbine sees on the given terrain.
     *
     * @param groundSpeed wind speed of the hour at ground level ({@code wind_ms}), m/s
     * @param terrain     terrain of the turbine's cell
     * @return the speed, increased by 15% on a mountain
     */
    public static double effectiveSpeed(double groundSpeed, Terrain terrain) {
        return terrain == Terrain.MOUNTAIN ? groundSpeed * MOUNTAIN_FACTOR : groundSpeed;
    }

    /**
     * Computes the output from the power curve.
     *
     * @param v wind speed at the turbine (after any terrain adjustment), m/s
     * @return output in kW
     */
    public double outputKw(double v) {
        if (v < cutIn || v >= cutOut) {
            return 0.0;
        }
        if (v >= rated) {
            return size;
        }
        double c3 = cutIn * cutIn * cutIn;
        return size * (v * v * v - c3) / (rated * rated * rated - c3);
    }
}
