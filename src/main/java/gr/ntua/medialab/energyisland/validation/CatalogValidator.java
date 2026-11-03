package gr.ntua.medialab.energyisland.validation;

import gr.ntua.medialab.energyisland.model.BatteryItem;
import gr.ntua.medialab.energyisland.model.Catalog;
import gr.ntua.medialab.energyisland.model.CatalogItem;
import gr.ntua.medialab.energyisland.model.PvItem;
import gr.ntua.medialab.energyisland.model.WindItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the value ranges of catalog items (scenario guide, section 4).
 * Duplicate ids and unknown types are reported by the loader, since a {@link Catalog}
 * cannot hold them.
 */
public final class CatalogValidator {

    /** File name used in the reported issues. */
    public static final String FILE = "catalog.json";

    private CatalogValidator() {
    }

    /**
     * Validates all items of a catalog.
     *
     * @param catalog the catalog to check
     * @return all issues found, empty if the catalog is valid
     */
    public static List<ValidationIssue> validate(Catalog catalog) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (CatalogItem item : catalog.items()) {
            issues.addAll(validate(item));
        }
        return issues;
    }

    /**
     * Validates one catalog item.
     *
     * @param item the item to check
     * @return all issues found, empty if the item is valid
     */
    public static List<ValidationIssue> validate(CatalogItem item) {
        List<ValidationIssue> issues = new ArrayList<>();
        String id = item.id();
        if (item.size() <= 0) {
            issues.add(issue(id, "size must be > 0, got " + item.size()));
        }
        if (item.capex() < 0) {
            issues.add(issue(id, "capex must be >= 0, got " + item.capex()));
        }
        if (item.life() <= 0) {
            issues.add(issue(id, "life must be > 0, got " + item.life()));
        }
        switch (item) {
            case PvItem pv -> { }
            case WindItem w -> {
                if (!(0 <= w.cutIn() && w.cutIn() < w.rated() && w.rated() < w.cutOut())) {
                    issues.add(issue(id, "wind speeds must satisfy 0 <= cutin < rated < cutout, got "
                            + w.cutIn() + " / " + w.rated() + " / " + w.cutOut()));
                }
            }
            case BatteryItem b -> {
                if (b.pmax() <= 0) {
                    issues.add(issue(id, "pmax must be > 0, got " + b.pmax()));
                }
                if (!(b.efficiency() > 0 && b.efficiency() <= 1)) {
                    issues.add(issue(id, "eff must satisfy 0 < eff <= 1, got " + b.efficiency()));
                }
                if (!(0 <= b.socMin() && b.socMin() < b.socMax() && b.socMax() <= 1)) {
                    issues.add(issue(id, "must satisfy 0 <= soc_min < soc_max <= 1, got soc_min "
                            + b.socMin() + ", soc_max " + b.socMax()));
                }
            }
        }
        return issues;
    }

    private static ValidationIssue issue(String id, String message) {
        return new ValidationIssue(FILE, id, message);
    }
}
