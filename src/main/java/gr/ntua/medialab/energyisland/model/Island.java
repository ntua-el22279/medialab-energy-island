package gr.ntua.medialab.energyisland.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The island ({@code island.json}): the terrain grid, the investment budget, the existing
 * units and the candidate sites.
 *
 * <p>This class is mutable so the editor can change it. It does not reject invalid states
 * (an element on sea, two elements on one cell, ...): those are reported by validation so
 * they can be highlighted on the map. Units and sites are immutable records; editing one
 * means replacing it. The order of {@link #sites()} is kept, because the controller uses
 * batteries in site order.
 */
public final class Island {

    private String name;
    private final int width;
    private final int height;
    private double budget;
    private final Terrain[][] cells;
    private final List<Unit> units = new ArrayList<>();
    private final List<Site> sites = new ArrayList<>();

    /**
     * Creates an island with every cell set to the same terrain and no units or sites.
     *
     * @param name   display name
     * @param width  number of columns, &gt; 0
     * @param height number of rows, &gt; 0
     * @param budget maximum total investment for new units, EUR
     * @param fill   initial terrain of every cell
     * @throws IllegalArgumentException if width or height is not positive
     */
    public Island(String name, int width, int height, double budget, Terrain fill) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid size must be positive: " + width + "x" + height);
        }
        this.name = name;
        this.width = width;
        this.height = height;
        this.budget = budget;
        this.cells = new Terrain[height][width];
        for (Terrain[] row : cells) {
            java.util.Arrays.fill(row, fill);
        }
    }

    /**
     * Returns a deep copy of this island (grid and element lists).
     *
     * @return an independent copy
     */
    public Island copy() {
        Island c = new Island(name, width, height, budget, Terrain.SEA);
        for (int y = 0; y < height; y++) {
            System.arraycopy(cells[y], 0, c.cells[y], 0, width);
        }
        c.units.addAll(units);
        c.sites.addAll(sites);
        return c;
    }

    // ---- basic properties ----

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @param name the new display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return number of columns */
    public int getWidth() {
        return width;
    }

    /** @return number of rows */
    public int getHeight() {
        return height;
    }

    /** @return maximum total investment for new units, EUR */
    public double getBudget() {
        return budget;
    }

    /** @param budget the new budget, EUR */
    public void setBudget(double budget) {
        this.budget = budget;
    }

    // ---- terrain ----

    /**
     * Tells whether a position lies on the grid.
     *
     * @param p a position
     * @return {@code true} if {@code 0 <= x < width} and {@code 0 <= y < height}
     */
    public boolean contains(Position p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }

    /**
     * Returns the terrain of a cell.
     *
     * @param p a position on the grid
     * @return its terrain
     * @throws IndexOutOfBoundsException if the position is off the grid
     */
    public Terrain terrainAt(Position p) {
        requireOnGrid(p);
        return cells[p.y()][p.x()];
    }

    /**
     * Changes the terrain of a cell.
     *
     * @param p       a position on the grid
     * @param terrain the new terrain
     * @throws IndexOutOfBoundsException if the position is off the grid
     */
    public void setTerrain(Position p, Terrain terrain) {
        requireOnGrid(p);
        cells[p.y()][p.x()] = terrain;
    }

    private void requireOnGrid(Position p) {
        if (!contains(p)) {
            throw new IndexOutOfBoundsException("Position " + p + " is outside " + width + "x" + height);
        }
    }

    // ---- units and sites ----

    /** @return existing units in file order (read-only) */
    public List<Unit> units() {
        return Collections.unmodifiableList(units);
    }

    /** @return candidate sites in file order (read-only) */
    public List<Site> sites() {
        return Collections.unmodifiableList(sites);
    }

    /**
     * Returns all units and sites.
     *
     * @return units followed by sites
     */
    public List<PlacedElement> elements() {
        List<PlacedElement> all = new ArrayList<>(units);
        all.addAll(sites);
        return all;
    }

    /**
     * Returns the elements on a cell. Normally zero or one; more means the island is invalid.
     *
     * @param p a position
     * @return the elements at {@code p}
     */
    public List<PlacedElement> elementsAt(Position p) {
        return elements().stream().filter(e -> e.position().equals(p)).toList();
    }

    /**
     * Finds a unit or site by id.
     *
     * @param id element id
     * @return the first element with that id, or empty
     */
    public Optional<PlacedElement> findElement(String id) {
        return elements().stream().filter(e -> e.id().equals(id)).findFirst();
    }

    /**
     * Finds a candidate site by id.
     *
     * @param id site id
     * @return the site, or empty
     */
    public Optional<Site> findSite(String id) {
        return sites.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /**
     * Returns the diesel plant. A valid island has exactly one.
     *
     * @return the first diesel plant, or empty if there is none
     */
    public Optional<DieselPlant> diesel() {
        return units.stream().filter(DieselPlant.class::isInstance)
                .map(DieselPlant.class::cast).findFirst();
    }

    /**
     * Returns the loads.
     *
     * @return all loads in file order
     */
    public List<Load> loads() {
        return units.stream().filter(Load.class::isInstance).map(Load.class::cast).toList();
    }

    /**
     * Adds a unit or site at the end of its list.
     *
     * @param element the element to add
     */
    public void add(PlacedElement element) {
        switch (element) {
            case Unit u -> units.add(u);
            case Site s -> sites.add(s);
        }
    }

    /**
     * Replaces the element with the given id, keeping its place in the list
     * (used when editing parameters, moving, or renaming).
     *
     * @param id      id of the element to replace
     * @param updated the new element; must be of the same kind (unit or site)
     * @return {@code true} if an element was replaced
     * @throws IllegalArgumentException if a unit would be replaced by a site or vice versa
     */
    public boolean replace(String id, PlacedElement updated) {
        int i = indexOf(units, id);
        if (i >= 0) {
            if (!(updated instanceof Unit u)) {
                throw new IllegalArgumentException("Cannot replace unit " + id + " with a site");
            }
            units.set(i, u);
            return true;
        }
        i = indexOf(sites, id);
        if (i >= 0) {
            if (!(updated instanceof Site s)) {
                throw new IllegalArgumentException("Cannot replace site " + id + " with a unit");
            }
            sites.set(i, s);
            return true;
        }
        return false;
    }

    /**
     * Moves an element to another cell.
     *
     * @param id     element id
     * @param target the new position
     * @return {@code true} if the element was found
     */
    public boolean move(String id, Position target) {
        return findElement(id).map(e -> replace(id, e.withPosition(target))).orElse(false);
    }

    /**
     * Removes the element with the given id.
     *
     * @param id element id
     * @return {@code true} if an element was removed
     */
    public boolean remove(String id) {
        return units.removeIf(u -> u.id().equals(id)) | sites.removeIf(s -> s.id().equals(id));
    }

    private static int indexOf(List<? extends PlacedElement> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
