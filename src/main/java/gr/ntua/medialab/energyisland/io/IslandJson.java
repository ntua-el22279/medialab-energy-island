package gr.ntua.medialab.energyisland.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gr.ntua.medialab.energyisland.model.DieselPlant;
import gr.ntua.medialab.energyisland.model.Island;
import gr.ntua.medialab.energyisland.model.Load;
import gr.ntua.medialab.energyisland.model.PlacedElement;
import gr.ntua.medialab.energyisland.model.Position;
import gr.ntua.medialab.energyisland.model.Site;
import gr.ntua.medialab.energyisland.model.TechType;
import gr.ntua.medialab.energyisland.model.Terrain;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Reads {@code island.json}: checks field presence and types, the map shape and symbols,
 * and unit/technology names. Placement rules and value ranges are checked afterwards
 * by {@link gr.ntua.medialab.energyisland.validation.IslandValidator}.
 */
final class IslandJson {

    static final String FILE = "island.json";

    private IslandJson() {
    }

    /**
     * Reads the island file.
     *
     * @return the island, or empty if it could not be built (the reasons are in {@code issues})
     */
    static Optional<Island> read(Path path, List<ValidationIssue> issues) {
        Optional<JsonElement> root = JsonFiles.parse(path, FILE, issues);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        JsonFields f = new JsonFields(FILE, issues);
        Optional<JsonObject> rootObj = f.asObject(root.get(), null, "the top level");
        if (rootObj.isEmpty()) {
            return Optional.empty();
        }
        JsonObject o = rootObj.get();
        int before = issues.size();

        Optional<String> name = f.string(o, "name", "name");
        Optional<Integer> width = f.integer(o, "width", "width");
        Optional<Integer> height = f.integer(o, "height", "height");
        Optional<Double> budget = f.number(o, "budget", "budget");
        Optional<JsonArray> map = f.array(o, "map", "map");
        width.filter(w -> w <= 0).ifPresent(w -> f.error("width", "width must be > 0, got " + w));
        height.filter(h -> h <= 0).ifPresent(h -> f.error("height", "height must be > 0, got " + h));

        Terrain[][] cells = null;
        if (map.isPresent() && width.isPresent() && height.isPresent() && width.get() > 0 && height.get() > 0) {
            cells = readMap(map.get(), width.get(), height.get(), f);
        }
        List<PlacedElement> elements = new ArrayList<>();
        f.array(o, "units", "units").ifPresent(a -> readUnits(a, f, elements));
        f.array(o, "sites", "sites").ifPresent(a -> readSites(a, f, elements));

        if (issues.size() > before || cells == null) {
            return Optional.empty();
        }
        Island island = new Island(name.get(), width.get(), height.get(), budget.get(), Terrain.SEA);
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[y].length; x++) {
                island.setTerrain(new Position(x, y), cells[y][x]);
            }
        }
        elements.forEach(island::add);
        return Optional.of(island);
    }

    private static Terrain[][] readMap(JsonArray map, int width, int height, JsonFields f) {
        if (map.size() != height) {
            f.error("map", "map has " + map.size() + " rows, expected height = " + height);
            return null;
        }
        Terrain[][] cells = new Terrain[height][width];
        boolean ok = true;
        for (int y = 0; y < height; y++) {
            JsonElement row = map.get(y);
            String element = "map row " + y;
            if (!row.isJsonPrimitive() || !row.getAsJsonPrimitive().isString()) {
                f.error(element, "map rows must be strings");
                ok = false;
                continue;
            }
            String s = row.getAsString();
            if (s.length() != width) {
                f.error(element, "row has " + s.length() + " characters, expected width = " + width);
                ok = false;
                continue;
            }
            for (int x = 0; x < width; x++) {
                Optional<Terrain> t = Terrain.fromSymbol(s.charAt(x));
                if (t.isEmpty()) {
                    f.error(element, "unknown symbol '" + s.charAt(x) + "' at column " + x
                            + " (expected ~ . ^ H)");
                    ok = false;
                } else {
                    cells[y][x] = t.get();
                }
            }
        }
        return ok ? cells : null;
    }

    private static Optional<Position> position(JsonObject o, String element, JsonFields f) {
        Optional<Integer> x = f.integer(o, "x", element);
        Optional<Integer> y = f.integer(o, "y", element);
        return x.isPresent() && y.isPresent() ? Optional.of(new Position(x.get(), y.get())) : Optional.empty();
    }

    private static void readUnits(JsonArray units, JsonFields f, List<PlacedElement> out) {
        for (int i = 0; i < units.size(); i++) {
            String element = JsonFields.label(units.get(i), "units", i);
            Optional<JsonObject> obj = f.asObject(units.get(i), element, "a unit");
            if (obj.isEmpty()) {
                continue;
            }
            JsonObject u = obj.get();
            Optional<String> id = f.string(u, "id", element);
            Optional<String> type = f.string(u, "type", element);
            Optional<Position> pos = position(u, element, f);
            if (type.isEmpty()) {
                continue;
            }
            switch (type.get()) {
                case "DIESEL" -> {
                    Optional<Double> pmin = f.number(u, "pmin", element);
                    Optional<Double> pmax = f.number(u, "pmax", element);
                    Optional<Double> fuel = f.number(u, "fuel", element);
                    Optional<Double> co2 = f.number(u, "co2", element);
                    Optional<Double> start = f.number(u, "start", element);
                    if (id.isPresent() && pos.isPresent() && pmin.isPresent() && pmax.isPresent()
                            && fuel.isPresent() && co2.isPresent() && start.isPresent()) {
                        out.add(new DieselPlant(id.get(), pos.get(), pmin.get(), pmax.get(),
                                fuel.get(), co2.get(), start.get()));
                    }
                }
                case "LOAD" -> {
                    Optional<String> category = f.string(u, "category", element);
                    Optional<Double> scale = f.number(u, "scale", element);
                    if (id.isPresent() && pos.isPresent() && category.isPresent() && scale.isPresent()) {
                        out.add(new Load(id.get(), pos.get(), category.get(), scale.get()));
                    }
                }
                default -> f.error(element, "unknown unit type '" + type.get() + "' (expected DIESEL or LOAD)");
            }
        }
    }

    private static void readSites(JsonArray sites, JsonFields f, List<PlacedElement> out) {
        for (int i = 0; i < sites.size(); i++) {
            String element = JsonFields.label(sites.get(i), "sites", i);
            Optional<JsonObject> obj = f.asObject(sites.get(i), element, "a site");
            if (obj.isEmpty()) {
                continue;
            }
            JsonObject s = obj.get();
            Optional<String> id = f.string(s, "id", element);
            Optional<Position> pos = position(s, element, f);
            Optional<JsonArray> allowed = f.array(s, "allowed", element);
            if (allowed.isEmpty()) {
                continue;
            }
            EnumSet<TechType> types = EnumSet.noneOf(TechType.class);
            boolean ok = true;
            for (JsonElement a : allowed.get()) {
                Optional<TechType> t = a.isJsonPrimitive() && a.getAsJsonPrimitive().isString()
                        ? TechType.fromName(a.getAsString()) : Optional.empty();
                if (t.isEmpty()) {
                    f.error(element, "unknown technology " + a + " in 'allowed' (expected PV, WIND or BATTERY)");
                    ok = false;
                } else {
                    types.add(t.get());
                }
            }
            if (ok && id.isPresent() && pos.isPresent()) {
                out.add(new Site(id.get(), pos.get(), types));
            }
        }
    }
}
