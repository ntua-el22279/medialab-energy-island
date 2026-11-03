package gr.ntua.medialab.energyisland.model;

/**
 * Something that can be built at a site ({@code items} in {@code catalog.json}).
 * The same item may be built at several sites.
 */
public sealed interface CatalogItem permits PvItem, WindItem, BatteryItem {

    /**
     * Returns the unique catalog id.
     *
     * @return the item id, e.g. {@code pv100}
     */
    String id();

    /**
     * Returns the technology of this item.
     *
     * @return the technology type
     */
    TechType type();

    /**
     * Returns the item size.
     *
     * @return rated power in kW for PV/WIND, capacity in kWh for BATTERY
     */
    double size();

    /**
     * Returns the investment cost.
     *
     * @return capital cost in EUR
     */
    double capex();

    /**
     * Returns the lifetime.
     *
     * @return lifetime in years
     */
    double life();

    /**
     * Returns the yearly share of the investment, used in the annual cost.
     *
     * @return {@code capex / life}, in EUR per year
     */
    default double annualizedCapex() {
        return capex() / life();
    }
}
