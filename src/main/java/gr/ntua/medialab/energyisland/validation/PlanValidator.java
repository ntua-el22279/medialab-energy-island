package gr.ntua.medialab.energyisland.validation;

import gr.ntua.medialab.energyisland.model.Catalog;
import gr.ntua.medialab.energyisland.model.CatalogItem;
import gr.ntua.medialab.energyisland.model.Island;
import gr.ntua.medialab.energyisland.model.Plan;
import gr.ntua.medialab.energyisland.model.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks that a {@link Plan} fits an island and a catalog: every assigned site exists,
 * every item exists, and the item's technology is allowed at its site.
 * <p>
 * Staying within the budget is <em>not</em> checked here. A plan over budget is still
 * valid and can be simulated; it is only not <em>accepted</em>.
 */
public final class PlanValidator {

    /** Label used as the "file" of the reported issues. */
    public static final String FILE = "plan";

    private PlanValidator() {
    }

    /**
     * Validates a plan against an island and a catalog.
     *
     * @param plan    the plan to check
     * @param island  the island whose sites the plan refers to
     * @param catalog the catalog whose items the plan refers to
     * @return all problems found, empty if the plan is valid
     */
    public static List<ValidationIssue> validate(Plan plan, Island island, Catalog catalog) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (Map.Entry<String, String> e : plan.assignments().entrySet()) {
            String siteId = e.getKey();
            String itemId = e.getValue();
            Optional<Site> site = island.findSite(siteId);
            Optional<CatalogItem> item = catalog.find(itemId);
            if (site.isEmpty()) {
                issues.add(new ValidationIssue(FILE, siteId, "no candidate site with this id on the island"));
            }
            if (item.isEmpty()) {
                issues.add(new ValidationIssue(FILE, siteId, "unknown catalog item " + itemId));
            }
            if (site.isPresent() && item.isPresent() && !site.get().allows(item.get().type())) {
                issues.add(new ValidationIssue(FILE, siteId, "item " + itemId + " (" + item.get().type()
                        + ") is not allowed here; allowed: " + site.get().allowed()));
            }
        }
        return issues;
    }
}
