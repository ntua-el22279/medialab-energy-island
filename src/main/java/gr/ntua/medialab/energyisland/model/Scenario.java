package gr.ntua.medialab.energyisland.model;

import java.util.List;

/**
 * A loaded scenario: the contents of one folder {@code medialab/scenarios/<id>/}.
 */
public final class Scenario {

    private final String id;
    private final Island island;
    private final Catalog catalog;
    private final List<DayProfile> days;

    /**
     * Creates a scenario.
     *
     * @param id      folder name, used as the scenario id
     * @param island  the island (editable)
     * @param catalog the catalog
     * @param days    representative days in {@code weights.json} order
     */
    public Scenario(String id, Island island, Catalog catalog, List<DayProfile> days) {
        this.id = id;
        this.island = island;
        this.catalog = catalog;
        this.days = List.copyOf(days);
    }

    /** @return the scenario id (folder name) */
    public String id() {
        return id;
    }

    /** @return the island */
    public Island island() {
        return island;
    }

    /** @return the catalog */
    public Catalog catalog() {
        return catalog;
    }

    /** @return the representative days (read-only) */
    public List<DayProfile> days() {
        return days;
    }

    /**
     * Returns the sum of the day weights.
     *
     * @return total days represented; 365 for a valid scenario
     */
    public int totalWeight() {
        return days.stream().mapToInt(DayProfile::weight).sum();
    }
}
