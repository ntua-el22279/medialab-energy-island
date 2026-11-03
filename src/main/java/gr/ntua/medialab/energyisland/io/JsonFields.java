package gr.ntua.medialab.energyisland.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.util.List;
import java.util.Optional;

/**
 * Reads typed fields from a JSON object. Every missing field or wrong type is recorded
 * as a {@link ValidationIssue} for one file, and the getter returns empty.
 */
final class JsonFields {

    private final String file;
    private final List<ValidationIssue> issues;

    JsonFields(String file, List<ValidationIssue> issues) {
        this.file = file;
        this.issues = issues;
    }

    void error(String element, String message) {
        issues.add(new ValidationIssue(file, element, message));
    }

    Optional<JsonObject> asObject(JsonElement e, String element, String what) {
        if (e != null && e.isJsonObject()) {
            return Optional.of(e.getAsJsonObject());
        }
        error(element, what + " must be a JSON object");
        return Optional.empty();
    }

    private Optional<JsonElement> field(JsonObject o, String name, String element) {
        JsonElement e = o.get(name);
        if (e == null || e.isJsonNull()) {
            error(element, "missing required field '" + name + "'");
            return Optional.empty();
        }
        return Optional.of(e);
    }

    Optional<String> string(JsonObject o, String name, String element) {
        return field(o, name, element).flatMap(e -> {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                return Optional.of(e.getAsString());
            }
            error(element, "field '" + name + "' must be a string");
            return Optional.empty();
        });
    }

    Optional<Double> number(JsonObject o, String name, String element) {
        return field(o, name, element).flatMap(e -> {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                double d = e.getAsDouble();
                if (Double.isFinite(d)) {
                    return Optional.of(d);
                }
            }
            error(element, "field '" + name + "' must be a number");
            return Optional.empty();
        });
    }

    Optional<Integer> integer(JsonObject o, String name, String element) {
        return field(o, name, element).flatMap(e -> {
            Optional<Integer> i = asInteger(e);
            if (i.isEmpty()) {
                error(element, "field '" + name + "' must be an integer");
            }
            return i;
        });
    }

    /** Returns the value as an int if it is a JSON number with no fractional part. */
    static Optional<Integer> asInteger(JsonElement e) {
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isNumber()) {
                double d = p.getAsDouble();
                if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) {
                    return Optional.of((int) d);
                }
            }
        }
        return Optional.empty();
    }

    Optional<JsonArray> array(JsonObject o, String name, String element) {
        return field(o, name, element).flatMap(e -> {
            if (e.isJsonArray()) {
                return Optional.of(e.getAsJsonArray());
            }
            error(element, "field '" + name + "' must be an array");
            return Optional.empty();
        });
    }

    /**
     * Picks a readable label for an array element: its {@code id} if it has a string one,
     * otherwise {@code arrayName[index]}.
     */
    static String label(JsonElement e, String arrayName, int index) {
        if (e.isJsonObject()) {
            JsonElement id = e.getAsJsonObject().get("id");
            if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString() && !id.getAsString().isBlank()) {
                return id.getAsString();
            }
        }
        return arrayName + "[" + index + "]";
    }
}
