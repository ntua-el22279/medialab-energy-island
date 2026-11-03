package gr.ntua.medialab.energyisland.validation;

import gr.ntua.medialab.energyisland.model.DayProfile;
import gr.ntua.medialab.energyisland.model.DieselPlant;
import gr.ntua.medialab.energyisland.model.Island;
import gr.ntua.medialab.energyisland.model.Load;
import gr.ntua.medialab.energyisland.model.PlacedElement;
import gr.ntua.medialab.energyisland.model.Position;
import gr.ntua.medialab.energyisland.model.Site;
import gr.ntua.medialab.energyisland.model.Terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks an {@link Island} against the rules of the scenario guide: coordinates on the grid,
 * placement rules (section 3.5), exactly one diesel plant, non-empty {@code allowed}, value ranges,
 * and that every load category is a column of every day.
 */
public final class IslandValidator {

    /** File name used in the reported issues. */
    public static final String FILE = "island.json";

    private IslandValidator() {
    }

    /**
     * Validates an island.
     *
     * @param island the island to check
     * @param days   the scenario days, used to check load categories; may be empty
     * @return all issues found, empty if the island is valid
     */
    public static List<ValidationIssue> validate(Island island, List<DayProfile> days) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (island.getBudget() < 0) {
            issues.add(new ValidationIssue(FILE, "budget", "budget must be >= 0, got " + island.getBudget()));
        }
        checkIds(island, issues);
        checkPlacement(island, issues);
        checkDieselCount(island, issues);
        for (PlacedElement e : island.elements()) {
            switch (e) {
                case DieselPlant d -> checkDiesel(d, issues);
                case Load l -> checkLoad(l, days, issues);
                case Site s -> {
                    if (s.allowed().isEmpty()) {
                        issues.add(new ValidationIssue(FILE, s.id(), "site allows no technology"));
                    }
                }
            }
        }
        return issues;
    }

    private static void checkIds(Island island, List<ValidationIssue> issues) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PlacedElement e : island.elements()) {
            if (e.id() == null || e.id().isBlank()) {
                issues.add(new ValidationIssue(FILE, "at " + e.position(), "id must not be empty"));
            } else {
                counts.merge(e.id(), 1, Integer::sum);
            }
        }
        counts.forEach((id, n) -> {
            if (n > 1) {
                issues.add(new ValidationIssue(FILE, id, "id is used by " + n + " units/sites; ids must be unique"));
            }
        });
    }

    private static void checkPlacement(Island island, List<ValidationIssue> issues) {
        Map<Position, PlacedElement> occupied = new HashMap<>();
        for (PlacedElement e : island.elements()) {
            Position p = e.position();
            if (!island.contains(p)) {
                issues.add(new ValidationIssue(FILE, e.id(), "position " + p + " is outside the "
                        + island.getWidth() + "x" + island.getHeight() + " grid"));
                continue;
            }
            PlacedElement other = occupied.putIfAbsent(p, e);
            if (other != null) {
                issues.add(new ValidationIssue(FILE, e.id(), "cell " + p + " is already used by " + other.id()));
            }
            Terrain t = island.terrainAt(p);
            String problem = switch (e) {
                case Load l -> t == Terrain.SETTLEMENT ? null : "a load must be on a settlement (H), not " + describe(t);
                case DieselPlant d -> isLandOrMountain(t) ? null : "the diesel plant must be on land (.) or mountain (^), not " + describe(t);
                case Site s -> isLandOrMountain(t) ? null : "a site must be on land (.) or mountain (^), not " + describe(t);
            };
            if (problem != null) {
                issues.add(new ValidationIssue(FILE, e.id(), problem + " at " + p));
            }
        }
    }

    private static boolean isLandOrMountain(Terrain t) {
        return t == Terrain.LAND || t == Terrain.MOUNTAIN;
    }

    private static String describe(Terrain t) {
        return t.name().toLowerCase() + " (" + t.symbol() + ")";
    }

    private static void checkDieselCount(Island island, List<ValidationIssue> issues) {
        long n = island.units().stream().filter(DieselPlant.class::isInstance).count();
        if (n != 1) {
            issues.add(new ValidationIssue(FILE, "units", "there must be exactly one diesel plant, found " + n));
        }
    }

    private static void checkDiesel(DieselPlant d, List<ValidationIssue> issues) {
        if (d.pmax() <= 0) {
            issues.add(new ValidationIssue(FILE, d.id(), "pmax must be > 0, got " + d.pmax()));
        }
        if (d.pmin() < 0 || d.pmin() > d.pmax()) {
            issues.add(new ValidationIssue(FILE, d.id(), "pmin must be between 0 and pmax (" + d.pmax() + "), got " + d.pmin()));
        }
        nonNegative(d.id(), "fuel", d.fuelCost(), issues);
        nonNegative(d.id(), "co2", d.co2(), issues);
        nonNegative(d.id(), "start", d.startCost(), issues);
    }

    private static void checkLoad(Load l, List<DayProfile> days, List<ValidationIssue> issues) {
        if (l.scale() <= 0) {
            issues.add(new ValidationIssue(FILE, l.id(), "scale must be > 0, got " + l.scale()));
        }
        if (l.category() == null || l.category().isBlank()) {
            issues.add(new ValidationIssue(FILE, l.id(), "category must not be empty"));
            return;
        }
        for (DayProfile day : days) {
            if (!day.hasCategory(l.category())) {
                issues.add(new ValidationIssue("days/" + day.name(), l.id(),
                        "no column '" + l.category() + "' for the category of load " + l.id()));
            }
        }
    }

    private static void nonNegative(String id, String field, double value, List<ValidationIssue> issues) {
        if (value < 0) {
            issues.add(new ValidationIssue(FILE, id, field + " must be >= 0, got " + value));
        }
    }
}
