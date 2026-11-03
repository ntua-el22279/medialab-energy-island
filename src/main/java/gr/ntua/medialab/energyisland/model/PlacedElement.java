package gr.ntua.medialab.energyisland.model;

/**
 * Anything that occupies one cell of the map: an existing unit or a candidate site.
 * At most one element may occupy a cell (checked by validation, not enforced here,
 * so the editor can show invalid states).
 */
public sealed interface PlacedElement permits Unit, Site {

    /**
     * Returns the identifier, unique among all units and sites of an island.
     *
     * @return the element id
     */
    String id();

    /**
     * Returns the cell this element occupies.
     *
     * @return the position on the grid
     */
    Position position();

    /**
     * Returns a copy of this element moved to another cell.
     *
     * @param newPosition the target cell
     * @return a new element equal to this one except for its position
     */
    PlacedElement withPosition(Position newPosition);
}
