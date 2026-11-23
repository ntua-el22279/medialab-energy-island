package gr.ntua.medialab.energyisland.simulation;

/**
 * The controller rule that decided an hour (brief A.1, "Ελεγκτής λειτουργίας").
 * Rule 4 (blackout) is not a separate case: it shows up as unserved energy in any rule.
 */
public enum ControlRule {
    /** Rule 1, {@code N <= 0}: batteries charge with the surplus, the rest is curtailed, diesel off. */
    SURPLUS,
    /** Rule 2, {@code 0 < N <= D_max}: batteries discharge to cover the deficit, diesel off. */
    BATTERIES,
    /** Rule 3, {@code N > D_max}: diesel runs; batteries cover only {@code N - P_D}. */
    DIESEL
}
