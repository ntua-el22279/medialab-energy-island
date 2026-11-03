package gr.ntua.medialab.energyisland.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A representative day: 24 hourly values of solar factor, wind speed and one
 * normalised demand profile per load category, plus how many days of the year it stands for.
 * Immutable.
 */
public final class DayProfile {

    /** Number of hourly steps in a day. */
    public static final int HOURS = 24;

    private final String name;
    private final int weight;
    private final double[] solar;
    private final double[] windSpeed;
    private final Map<String, double[]> loadProfiles;

    /**
     * Creates a day. Arrays are copied.
     *
     * @param name         the CSV file name, e.g. {@code winter.csv}
     * @param weight       days of the year this day represents ({@code weights.json})
     * @param solar        24 solar factors (0-1)
     * @param windSpeed    24 ground-level wind speeds, m/s
     * @param loadProfiles 24 values per load category column
     * @throws IllegalArgumentException if any array does not have 24 values
     */
    public DayProfile(String name, int weight, double[] solar, double[] windSpeed,
                      Map<String, double[]> loadProfiles) {
        this.name = name;
        this.weight = weight;
        this.solar = check(solar, "solar");
        this.windSpeed = check(windSpeed, "wind_ms");
        Map<String, double[]> copy = new LinkedHashMap<>();
        loadProfiles.forEach((category, values) -> copy.put(category, check(values, category)));
        this.loadProfiles = Collections.unmodifiableMap(copy);
    }

    private static double[] check(double[] values, String column) {
        if (values.length != HOURS) {
            throw new IllegalArgumentException(
                    "Column " + column + " has " + values.length + " values, expected " + HOURS);
        }
        return values.clone();
    }

    /**
     * Returns the day name.
     *
     * @return the CSV file name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the weight of the day.
     *
     * @return days of the year represented
     */
    public int weight() {
        return weight;
    }

    /**
     * Returns the solar factor of an hour.
     *
     * @param hour 0-23
     * @return solar factor (0-1)
     */
    public double solar(int hour) {
        return solar[hour];
    }

    /**
     * Returns the ground-level wind speed of an hour.
     *
     * @param hour 0-23
     * @return wind speed, m/s
     */
    public double windSpeed(int hour) {
        return windSpeed[hour];
    }

    /**
     * Tells whether the day has a profile for a load category.
     *
     * @param category load category, e.g. {@code village}
     * @return {@code true} if the CSV had that column
     */
    public boolean hasCategory(String category) {
        return loadProfiles.containsKey(category);
    }

    /**
     * Returns the load categories present in the day.
     *
     * @return the category column names (read-only)
     */
    public Set<String> categories() {
        return loadProfiles.keySet();
    }

    /**
     * Returns the normalised demand of a category for an hour.
     *
     * @param category load category
     * @param hour     0-23
     * @return profile value
     * @throws IllegalArgumentException if the category is not in this day
     */
    public double loadProfile(String category, int hour) {
        double[] values = loadProfiles.get(category);
        if (values == null) {
            throw new IllegalArgumentException("Day " + name + " has no column " + category);
        }
        return values[hour];
    }

    @Override
    public String toString() {
        return name + " (x" + weight + ", " + Arrays.toString(categories().toArray()) + ")";
    }
}
