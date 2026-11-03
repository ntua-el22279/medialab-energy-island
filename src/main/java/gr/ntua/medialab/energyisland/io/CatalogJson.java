package gr.ntua.medialab.energyisland.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gr.ntua.medialab.energyisland.model.BatteryItem;
import gr.ntua.medialab.energyisland.model.Catalog;
import gr.ntua.medialab.energyisland.model.CatalogItem;
import gr.ntua.medialab.energyisland.model.PvItem;
import gr.ntua.medialab.energyisland.model.WindItem;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads {@code catalog.json}: field presence and types, known item types and unique ids.
 * Value ranges are checked afterwards by
 * {@link gr.ntua.medialab.energyisland.validation.CatalogValidator}.
 */
final class CatalogJson {

    static final String FILE = "catalog.json";

    private CatalogJson() {
    }

    /**
     * Reads the catalog file.
     *
     * @return the catalog, or empty if it could not be built (the reasons are in {@code issues})
     */
    static Optional<Catalog> read(Path path, List<ValidationIssue> issues) {
        Optional<JsonElement> root = JsonFiles.parse(path, FILE, issues);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        JsonFields f = new JsonFields(FILE, issues);
        Optional<JsonArray> items = f.asObject(root.get(), null, "the top level")
                .flatMap(o -> f.array(o, "items", "items"));
        if (items.isEmpty()) {
            return Optional.empty();
        }
        int before = issues.size();
        List<CatalogItem> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < items.get().size(); i++) {
            String element = JsonFields.label(items.get().get(i), "items", i);
            f.asObject(items.get().get(i), element, "an item")
                    .flatMap(o -> readItem(o, element, f))
                    .ifPresent(item -> {
                        if (ids.add(item.id())) {
                            result.add(item);
                        } else {
                            f.error(item.id(), "duplicate catalog id");
                        }
                    });
        }
        return issues.size() > before ? Optional.empty() : Optional.of(new Catalog(result));
    }

    private static Optional<CatalogItem> readItem(JsonObject o, String element, JsonFields f) {
        Optional<String> id = f.string(o, "id", element);
        Optional<String> type = f.string(o, "type", element);
        Optional<Double> size = f.number(o, "size", element);
        Optional<Double> capex = f.number(o, "capex", element);
        Optional<Double> life = f.number(o, "life", element);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        boolean common = id.isPresent() && size.isPresent() && capex.isPresent() && life.isPresent();
        switch (type.get()) {
            case "PV" -> {
                return common ? Optional.of(new PvItem(id.get(), size.get(), capex.get(), life.get()))
                        : Optional.empty();
            }
            case "WIND" -> {
                Optional<Double> cutin = f.number(o, "cutin", element);
                Optional<Double> rated = f.number(o, "rated", element);
                Optional<Double> cutout = f.number(o, "cutout", element);
                if (common && cutin.isPresent() && rated.isPresent() && cutout.isPresent()) {
                    return Optional.of(new WindItem(id.get(), size.get(), capex.get(), life.get(),
                            cutin.get(), rated.get(), cutout.get()));
                }
                return Optional.empty();
            }
            case "BATTERY" -> {
                Optional<Double> pmax = f.number(o, "pmax", element);
                Optional<Double> eff = f.number(o, "eff", element);
                Optional<Double> socMin = f.number(o, "soc_min", element);
                Optional<Double> socMax = f.number(o, "soc_max", element);
                if (common && pmax.isPresent() && eff.isPresent() && socMin.isPresent() && socMax.isPresent()) {
                    return Optional.of(new BatteryItem(id.get(), size.get(), capex.get(), life.get(),
                            pmax.get(), eff.get(), socMin.get(), socMax.get()));
                }
                return Optional.empty();
            }
            default -> {
                f.error(element, "unknown item type '" + type.get() + "' (expected PV, WIND or BATTERY)");
                return Optional.empty();
            }
        }
    }
}
