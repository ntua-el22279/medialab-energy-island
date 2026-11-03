package gr.ntua.medialab.energyisland.model;

import java.util.Optional;

/**
 * Terrain type of a map cell, with the symbol used for it in {@code island.json}.
 */
public enum Terrain {
    /** Sea ({@code ~}): nothing can be placed here. */
    SEA('~'),
    /** Land ({@code .}): diesel plant and candidate sites. */
    LAND('.'),
    /** Mountain ({@code ^}): like land, but wind speed is increased for turbines. */
    MOUNTAIN('^'),
    /** Settlement ({@code H}): loads only. */
    SETTLEMENT('H');

    private final char symbol;

    Terrain(char symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the map symbol of this terrain.
     *
     * @return the character used in the {@code map} rows
     */
    public char symbol() {
        return symbol;
    }

    /**
     * Looks up the terrain for a map symbol.
     *
     * @param symbol a character from a {@code map} row
     * @return the matching terrain, or empty if the symbol is unknown
     */
    public static Optional<Terrain> fromSymbol(char symbol) {
        for (Terrain t : values()) {
            if (t.symbol == symbol) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
