package gr.ntua.medialab.energyisland.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gr.ntua.medialab.energyisland.model.DayProfile;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads {@code days/weights.json} and the day CSV files it lists.
 */
final class DayFiles {

    static final String DIR = "days";
    static final String WEIGHTS_FILE = "weights.json";
    static final int DAYS_PER_YEAR = 365;

    private static final String HOUR = "hour";
    private static final String SOLAR = "solar";
    private static final String WIND = "wind_ms";
    /** Plain decimal numbers with a dot separator, e.g. {@code 12}, {@code 0.75}, {@code -1e-3}. */
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    private DayFiles() {
    }

    /**
     * Reads all days listed in {@code weights.json}, in file order.
     *
     * @param daysDir         the {@code days/} folder
     * @param usedCategories  load categories used by the island; other columns are ignored.
     *                        {@code null} if unknown (then every extra column is read)
     * @param issues          receives every problem found
     * @return the days that were read successfully
     */
    static List<DayProfile> read(Path daysDir, Set<String> usedCategories, List<ValidationIssue> issues) {
        String weightsLabel = DIR + "/" + WEIGHTS_FILE;
        Optional<JsonElement> root = JsonFiles.parse(daysDir.resolve(WEIGHTS_FILE), weightsLabel, issues);
        if (root.isEmpty()) {
            return List.of();
        }
        JsonFields f = new JsonFields(weightsLabel, issues);
        Optional<JsonElement> weights = f.asObject(root.get(), null, "the top level")
                .map(o -> o.get("weights"));
        if (weights.isEmpty()) {
            return List.of();
        }
        if (weights.get() == null || !weights.get().isJsonObject()) {
            f.error("weights", weights.get() == null ? "missing required field 'weights'"
                    : "field 'weights' must be an object mapping CSV file names to day counts");
            return List.of();
        }
        JsonObject map = weights.get().getAsJsonObject();
        List<DayProfile> days = new ArrayList<>();
        int sum = 0;
        boolean allWeightsValid = true;
        for (Map.Entry<String, JsonElement> e : map.entrySet()) {
            String fileName = e.getKey();
            Optional<Integer> w = JsonFields.asInteger(e.getValue()).filter(v -> v > 0);
            if (w.isEmpty()) {
                f.error(fileName, "weight must be a positive integer, got " + e.getValue());
                allWeightsValid = false;
            } else {
                sum += w.get();
            }
            Path csv = daysDir.resolve(fileName).normalize();
            if (fileName.isBlank() || !csv.getParent().equals(daysDir.normalize())) {
                f.error(fileName, "file name must refer to a file directly inside the days/ folder");
                continue;
            }
            readCsv(csv, DIR + "/" + fileName, w.orElse(0), usedCategories, issues).ifPresent(days::add);
        }
        if (map.isEmpty()) {
            f.error("weights", "no days are listed");
        } else if (allWeightsValid && sum != DAYS_PER_YEAR) {
            f.error("weights", "weights add up to " + sum + ", expected " + DAYS_PER_YEAR);
        }
        return days;
    }

    private static Optional<DayProfile> readCsv(Path path, String label, int weight,
                                                Set<String> usedCategories, List<ValidationIssue> issues) {
        Optional<String> text = JsonFiles.readText(path, label, issues);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        String[] lines = text.get().split("\\R", -1);
        int before = issues.size();

        // header: first non-blank line
        int lineNo = 0;
        while (lineNo < lines.length && lines[lineNo].isBlank()) {
            lineNo++;
        }
        if (lineNo == lines.length) {
            issues.add(new ValidationIssue(label, null, "file is empty"));
            return Optional.empty();
        }
        String[] header = splitRow(lines[lineNo]);
        Map<String, Integer> col = new HashMap<>();
        for (int c = 0; c < header.length; c++) {
            if (col.putIfAbsent(header[c], c) != null) {
                issues.add(new ValidationIssue(label, "line " + (lineNo + 1), "duplicate column '" + header[c] + "'"));
            }
        }
        for (String required : List.of(HOUR, SOLAR, WIND)) {
            if (!col.containsKey(required)) {
                issues.add(new ValidationIssue(label, "line " + (lineNo + 1), "missing column '" + required + "'"));
            }
        }
        if (issues.size() > before) {
            return Optional.empty();
        }
        Map<String, Integer> profileCols = new LinkedHashMap<>();
        for (int c = 0; c < header.length; c++) {
            String name = header[c];
            boolean fixed = name.equals(HOUR) || name.equals(SOLAR) || name.equals(WIND);
            if (!fixed && (usedCategories == null || usedCategories.contains(name))) {
                profileCols.put(name, c);
            }
        }

        double[] solar = new double[DayProfile.HOURS];
        double[] wind = new double[DayProfile.HOURS];
        Map<String, double[]> profiles = new LinkedHashMap<>();
        profileCols.keySet().forEach(k -> profiles.put(k, new double[DayProfile.HOURS]));

        int rows = 0;
        for (int i = lineNo + 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String where = "line " + (i + 1);
            String[] cells = splitRow(lines[i]);
            if (cells.length != header.length) {
                issues.add(new ValidationIssue(label, where, "has " + cells.length + " values, expected "
                        + header.length + " (one per header column)"));
                rows++;
                continue;
            }
            if (rows < DayProfile.HOURS) {
                Double hour = number(cells[col.get(HOUR)], HOUR, label, where, issues);
                if (hour != null && hour != rows) {
                    issues.add(new ValidationIssue(label, where, "hour is " + cells[col.get(HOUR)]
                            + ", expected " + rows + " (hours must be 0 to 23 in order)"));
                }
                Double s = number(cells[col.get(SOLAR)], SOLAR, label, where, issues);
                if (s != null && (s < 0 || s > 1)) {
                    issues.add(new ValidationIssue(label, where, "solar must be between 0 and 1, got " + s));
                }
                Double w = number(cells[col.get(WIND)], WIND, label, where, issues);
                if (w != null && w < 0) {
                    issues.add(new ValidationIssue(label, where, "wind_ms must be >= 0, got " + w));
                }
                if (s != null) {
                    solar[rows] = s;
                }
                if (w != null) {
                    wind[rows] = w;
                }
                for (Map.Entry<String, Integer> pc : profileCols.entrySet()) {
                    Double v = number(cells[pc.getValue()], pc.getKey(), label, where, issues);
                    if (v != null && v < 0) {
                        issues.add(new ValidationIssue(label, where, pc.getKey() + " must be >= 0, got " + v));
                    }
                    if (v != null) {
                        profiles.get(pc.getKey())[rows] = v;
                    }
                }
            }
            rows++;
        }
        if (rows != DayProfile.HOURS) {
            issues.add(new ValidationIssue(label, null, "has " + rows + " data rows, expected "
                    + DayProfile.HOURS + " (hours 0 to 23)"));
        }
        if (issues.size() > before) {
            return Optional.empty();
        }
        String name = path.getFileName().toString();
        return Optional.of(new DayProfile(name, weight, solar, wind, profiles));
    }

    private static String[] splitRow(String line) {
        String[] parts = line.split(",", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private static Double number(String text, String column, String label, String where,
                                 List<ValidationIssue> issues) {
        if (NUMBER.matcher(text).matches()) {
            return Double.parseDouble(text);
        }
        issues.add(new ValidationIssue(label, where, "column '" + column + "': '" + text + "' is not a number"));
        return null;
    }
}
