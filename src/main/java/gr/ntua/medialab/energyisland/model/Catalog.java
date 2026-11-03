package gr.ntua.medialab.energyisland.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The read-only list of items that can be built ({@code catalog.json}), in file order.
 */
public final class Catalog {

    private final List<CatalogItem> items;
    private final Map<String, CatalogItem> byId;

    /**
     * Creates a catalog.
     *
     * @param items the items in file order
     * @throws IllegalArgumentException if two items share an id
     *         (the loader reports this as a validation error first)
     */
    public Catalog(List<? extends CatalogItem> items) {
        Map<String, CatalogItem> map = new LinkedHashMap<>();
        for (CatalogItem item : items) {
            if (map.putIfAbsent(item.id(), item) != null) {
                throw new IllegalArgumentException("Duplicate catalog id: " + item.id());
            }
        }
        this.items = List.copyOf(items);
        this.byId = Collections.unmodifiableMap(map);
    }

    /**
     * Returns all items.
     *
     * @return the items in file order (read-only)
     */
    public List<CatalogItem> items() {
        return items;
    }

    /**
     * Finds an item by id.
     *
     * @param id catalog id
     * @return the item, or empty if there is no such id
     */
    public Optional<CatalogItem> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Lists the items that may be built at a site, i.e. whose type the site allows.
     *
     * @param site the candidate site
     * @return matching items in file order
     */
    public List<CatalogItem> itemsAllowedAt(Site site) {
        return items.stream().filter(i -> site.allows(i.type())).toList();
    }
}
