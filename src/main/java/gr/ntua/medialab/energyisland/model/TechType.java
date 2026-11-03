package gr.ntua.medialab.energyisland.model;

import java.util.Optional;

/**
 * Technologies that can be built at candidate sites. The names match the
 * {@code type} and {@code allowed} values of the scenario files.
 */
public enum TechType {
    /** Photovoltaic park. */
    PV,
    /** Wind turbine. */
    WIND,
    /** Battery storage. */
    BATTERY;

    /**
     * Parses a technology name exactly as written in the scenario files.
     *
     * @param name e.g. {@code "PV"}
     * @return the technology, or empty if the name is unknown
     */
    public static Optional<TechType> fromName(String name) {
        for (TechType t : values()) {
            if (t.name().equals(name)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
