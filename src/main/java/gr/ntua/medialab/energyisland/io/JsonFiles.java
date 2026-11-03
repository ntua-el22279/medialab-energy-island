package gr.ntua.medialab.energyisland.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads files as UTF-8 text and parses strict JSON (no comments, trailing commas, NaN, ...).
 */
final class JsonFiles {

    private static final Pattern LOCATION = Pattern.compile("line \\d+ column \\d+");

    private JsonFiles() {
    }

    /**
     * Reads a whole file as UTF-8, dropping a leading byte-order mark.
     *
     * @return the text, or empty after reporting why it could not be read
     */
    static Optional<String> readText(Path path, String label, List<ValidationIssue> issues) {
        if (!Files.isRegularFile(path)) {
            issues.add(new ValidationIssue(label, null, "file is missing"));
            return Optional.empty();
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return Optional.of(text.startsWith("﻿") ? text.substring(1) : text);
        } catch (CharacterCodingException e) {
            issues.add(new ValidationIssue(label, null, "file is not valid UTF-8 text"));
        } catch (IOException e) {
            issues.add(new ValidationIssue(label, null, "cannot read file: " + e.getMessage()));
        }
        return Optional.empty();
    }

    /**
     * Reads and parses a JSON file.
     *
     * @return the JSON tree, or empty after reporting the problem
     */
    static Optional<JsonElement> parse(Path path, String label, List<ValidationIssue> issues) {
        Optional<String> text = readText(path, label, issues);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            JsonReader reader = new JsonReader(new StringReader(text.get()));
            reader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                issues.add(new ValidationIssue(label, null, "invalid JSON: unexpected content after the end of the document"));
                return Optional.empty();
            }
            return Optional.of(root);
        } catch (JsonParseException | IOException e) {
            issues.add(new ValidationIssue(label, null, "invalid JSON" + location(e)));
            return Optional.empty();
        }
    }

    private static String location(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null) {
                Matcher m = LOCATION.matcher(t.getMessage());
                if (m.find()) {
                    return " at " + m.group();
                }
            }
        }
        return e.getMessage() == null ? "" : ": " + e.getMessage();
    }
}
