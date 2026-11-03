package gr.ntua.medialab.energyisland.model;

/**
 * An existing unit of the island ({@code units} in {@code island.json}).
 * Existing units are not changed by plans.
 */
public sealed interface Unit extends PlacedElement permits DieselPlant, Load {
}
