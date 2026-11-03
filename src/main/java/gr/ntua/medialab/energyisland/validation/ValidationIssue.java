package gr.ntua.medialab.energyisland.validation;

/**
 * One validation error, reported to the user without stopping the application.
 *
 * @param file    file the error is in, relative to the scenario folder (e.g. {@code days/winter.csv})
 * @param element the element concerned (an id, {@code line 5}, {@code map row 3}, a field name),
 *                or {@code null} if the error concerns the whole file
 * @param message a clear description of the problem
 */
public record ValidationIssue(String file, String element, String message) {

    @Override
    public String toString() {
        return element == null ? file + ": " + message : file + " [" + element + "]: " + message;
    }
}
