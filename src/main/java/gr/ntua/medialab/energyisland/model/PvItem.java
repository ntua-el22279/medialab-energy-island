package gr.ntua.medialab.energyisland.model;

/**
 * A photovoltaic park. Output = {@code size x solar}; it is not controllable.
 *
 * @param id    catalog id
 * @param size  rated power, kW
 * @param capex investment, EUR
 * @param life  lifetime, years
 */
public record PvItem(String id, double size, double capex, double life) implements CatalogItem {

    @Override
    public TechType type() {
        return TechType.PV;
    }

    /**
     * Computes the output for one hour.
     *
     * @param solar solar factor of the hour (0-1)
     * @return output in kW
     */
    public double outputKw(double solar) {
        return size * solar;
    }
}
