package gr.ntua.medialab.energyisland.simulation;

import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when a plan does not fit the island or catalog it is simulated with
 * (see {@link gr.ntua.medialab.energyisland.validation.PlanValidator}).
 */
public final class InvalidPlanException extends RuntimeException {

    private final transient List<ValidationIssue> issues;

    /**
     * Creates the exception.
     *
     * @param issues the problems found; not empty
     */
    public InvalidPlanException(List<ValidationIssue> issues) {
        super("Invalid plan:\n" + issues.stream().map(i -> "  " + i).collect(Collectors.joining("\n")));
        this.issues = List.copyOf(issues);
    }

    /**
     * Returns the problems found.
     *
     * @return the issues
     */
    public List<ValidationIssue> issues() {
        return issues;
    }
}
