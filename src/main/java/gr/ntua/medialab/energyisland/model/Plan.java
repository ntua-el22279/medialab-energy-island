package gr.ntua.medialab.energyisland.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A design: for each candidate site, either nothing or one catalog item.
 * Stored by ids (site id to catalog item id) so it can be saved, reopened, and checked
 * against an island that may have changed since.
 */
public final class Plan {

    private final String scenarioId;
    private final Map<String, String> assignments = new LinkedHashMap<>();

    /**
     * Creates an empty plan (nothing built anywhere).
     *
     * @param scenarioId id of the scenario the plan belongs to
     */
    public Plan(String scenarioId) {
        this.scenarioId = Objects.requireNonNull(scenarioId);
    }

    /**
     * Returns a copy of this plan.
     *
     * @return an independent copy
     */
    public Plan copy() {
        Plan p = new Plan(scenarioId);
        p.assignments.putAll(assignments);
        return p;
    }

    /** @return the scenario id */
    public String scenarioId() {
        return scenarioId;
    }

    /**
     * Builds a catalog item at a site, replacing whatever was there.
     *
     * @param siteId site id
     * @param itemId catalog item id
     */
    public void assign(String siteId, String itemId) {
        assignments.put(Objects.requireNonNull(siteId), Objects.requireNonNull(itemId));
    }

    /**
     * Leaves a site empty.
     *
     * @param siteId site id
     */
    public void clear(String siteId) {
        assignments.remove(siteId);
    }

    /**
     * Returns what is built at a site.
     *
     * @param siteId site id
     * @return the catalog item id, or empty if nothing is built there
     */
    public Optional<String> itemAt(String siteId) {
        return Optional.ofNullable(assignments.get(siteId));
    }

    /**
     * Returns all non-empty assignments.
     *
     * @return site id to catalog item id (read-only)
     */
    public Map<String, String> assignments() {
        return Collections.unmodifiableMap(assignments);
    }

    /**
     * Computes the total investment of the plan. Unknown item ids are ignored here;
     * they are reported when the plan is checked against the scenario.
     *
     * @param catalog the scenario catalog
     * @return sum of {@code capex}, EUR
     */
    public double investment(Catalog catalog) {
        return assignments.values().stream()
                .flatMap(id -> catalog.find(id).stream())
                .mapToDouble(CatalogItem::capex).sum();
    }

    /**
     * Computes the yearly investment cost of the plan.
     *
     * @param catalog the scenario catalog
     * @return sum of {@code capex / life}, EUR per year
     */
    public double annualizedInvestment(Catalog catalog) {
        return assignments.values().stream()
                .flatMap(id -> catalog.find(id).stream())
                .mapToDouble(CatalogItem::annualizedCapex).sum();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Plan p && scenarioId.equals(p.scenarioId) && assignments.equals(p.assignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId, assignments);
    }

    @Override
    public String toString() {
        return "Plan[" + scenarioId + ", " + assignments + "]";
    }
}
