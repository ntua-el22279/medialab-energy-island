package gr.ntua.medialab.energyisland.io;

import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.util.List;

/**
 * Thrown when a scenario cannot be loaded. It carries every problem found, so they can all be
 * shown to the user at once.
 */
public class InvalidScenarioException extends Exception {

    private final String scenarioId;
    private final transient List<ValidationIssue> issues;

    /**
     * Creates the exception.
     *
     * @param scenarioId id (folder name) of the scenario
     * @param issues     the problems found; not empty
     */
    public InvalidScenarioException(String scenarioId, List<ValidationIssue> issues) {
        super(buildMessage(scenarioId, issues));
        this.scenarioId = scenarioId;
        this.issues = List.copyOf(issues);
    }

    private static String buildMessage(String scenarioId, List<ValidationIssue> issues) {
        StringBuilder sb = new StringBuilder("Scenario '").append(scenarioId).append("' has ")
                .append(issues.size()).append(issues.size() == 1 ? " error:" : " errors:");
        issues.forEach(i -> sb.append("\n - ").append(i));
        return sb.toString();
    }

    /** @return the scenario id */
    public String scenarioId() {
        return scenarioId;
    }

    /** @return the problems found (read-only) */
    public List<ValidationIssue> issues() {
        return issues;
    }
}
