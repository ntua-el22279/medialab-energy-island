// Validation regression check for the scenario loader.
// Run with: tools/check-validation.sh   (from the project root, after mvn compile)
// Copies a valid sample scenario to a temp folder, breaks ONE thing, and checks that loading
// fails with the expected message. Also checks that the valid samples load.

import gr.ntua.medialab.energyisland.io.*;
import gr.ntua.medialab.energyisland.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

Path SAMPLES = Path.of("medialab/scenarios");
Path TMP = Files.createTempDirectory("energy-island-validation");
int passed = 0, failed = 0, caseNo = 0;

interface Mutation { void apply(Path dir) throws Exception; }

Path copyOf(String scenario) throws Exception {
    Path dst = TMP.resolve(String.format("case%02d_%s", ++caseNo, scenario));
    try (Stream<Path> s = Files.walk(SAMPLES.resolve(scenario))) {
        for (Path p : s.toList()) {
            Path t = dst.resolve(SAMPLES.resolve(scenario).relativize(p).toString());
            if (Files.isDirectory(p)) Files.createDirectories(t); else Files.copy(p, t);
        }
    }
    return dst;
}

void edit(Path file, String regex, String replacement) throws Exception {
    String s = Files.readString(file);
    String r = s.replaceFirst(regex, replacement);
    if (r.equals(s)) throw new IllegalStateException("pattern not found in " + file.getFileName() + ": " + regex);
    Files.writeString(file, r);
}

void expectError(String name, String scenario, Mutation m, String expected) {
    try {
        Path dir = copyOf(scenario);
        m.apply(dir);
        new ScenarioLoader().load(dir);
        System.out.println("FAIL " + name + ": loaded, expected an error containing \"" + expected + "\"");
        failed++;
    } catch (InvalidScenarioException e) {
        Optional<String> hit = e.issues().stream().map(Object::toString).filter(i -> i.contains(expected)).findFirst();
        if (hit.isPresent()) {
            System.out.println("PASS " + name + "  ->  " + hit.get());
            passed++;
        } else {
            System.out.println("FAIL " + name + ": no issue contains \"" + expected + "\"; got:\n" + e.getMessage());
            failed++;
        }
    } catch (Exception e) {
        System.out.println("FAIL " + name + ": " + e);
        failed++;
    }
}

void expectOk(String name, String scenario, Mutation m) {
    try {
        Path dir = copyOf(scenario);
        m.apply(dir);
        Scenario sc = new ScenarioLoader().load(dir);
        System.out.println("PASS " + name + "  ->  loaded " + sc.island().getName() + ", " + sc.island().sites().size()
                + " sites, " + sc.catalog().items().size() + " items, " + sc.days().size() + " days, weight " + sc.totalWeight());
        passed++;
    } catch (Exception e) {
        System.out.println("FAIL " + name + ": " + e.getMessage());
        failed++;
    }
}

Path island(Path d) { return d.resolve("island.json"); }
Path catalog(Path d) { return d.resolve("catalog.json"); }
Path day(Path d, String f) { return d.resolve("days").resolve(f); }
Path weights(Path d) { return d.resolve("days/weights.json"); }

// ---- valid inputs ----
expectOk("sample a", "a", d -> {});
expectOk("sample b", "b", d -> {});
expectOk("UTF-8 BOM in island.json", "a", d -> Files.writeString(island(d), "\uFEFF" + Files.readString(island(d))));
expectOk("CRLF line endings in CSV", "a", d -> Files.writeString(day(d, "winter.csv"), Files.readString(day(d, "winter.csv")).replace("\n", "\r\n")));
expectOk("unused CSV column is ignored", "a", d -> Files.writeString(day(d, "winter.csv"),
        Files.readString(day(d, "winter.csv")).lines().map(l -> l + (l.startsWith("hour") ? ",school" : ",abc")).collect(Collectors.joining("\n"))));
expectOk("unlisted CSV in days/ is ignored", "a", d -> Files.writeString(day(d, "extra.csv"), "garbage"));

// ---- 1. missing files / bad JSON ----
expectError("missing catalog.json", "a", d -> Files.delete(catalog(d)), "catalog.json: file is missing");
expectError("missing weights.json", "a", d -> Files.delete(weights(d)), "days/weights.json: file is missing");
expectError("JSON syntax error", "a", d -> edit(island(d), "\"budget\": 900000,", "\"budget\": 900000,,"), "island.json: invalid JSON at line");
expectError("JSON comment", "a", d -> edit(catalog(d), "\\{", "{ // comment"), "catalog.json: invalid JSON");
// ---- 2. missing field / wrong type ----
expectError("missing budget", "a", d -> edit(island(d), "\\s*\"budget\": 900000,", ""), "missing required field 'budget'");
expectError("width as string", "a", d -> edit(island(d), "\"width\": 20", "\"width\": \"20\""), "field 'width' must be an integer");
expectError("x not integer", "a", d -> edit(island(d), "(\"id\": \"s1\",\\s*\"x\": )9", "$19.5"), "field 'x' must be an integer");
expectError("missing battery eff", "a", d -> edit(catalog(d), "\\s*\"eff\": 0.9,", ""), "[b300]: missing required field 'eff'");
// ---- 3. map shape / symbols ----
expectError("short map row", "a", d -> edit(island(d), "\"~~~~~~~~~~~~~~~~~~~~\"", "\"~~~~~~~~~~~~~~~~~~~\""), "expected width = 20");
expectError("missing map row", "a", d -> edit(island(d), "\\s*\"~~~~~~~~~~~~~~~~~~~~\",", ""), "expected height = 15");
expectError("unknown map symbol", "a", d -> edit(island(d), "~~~\\.\\.\\.HH", "~~~...HX"), "unknown symbol 'X' at column 7");
// ---- 4. coordinates ----
expectError("site off the grid", "a", d -> edit(island(d), "(\"id\": \"s1\",\\s*\"x\": )9", "$199"), "position (99, 5) is outside");
// ---- 5. placement rules ----
expectError("site on sea", "a", d -> edit(island(d), "(\"id\": \"s1\",\\s*\"x\": 9,\\s*\"y\": )5", "$10"), "a site must be on land");
expectError("load not on settlement", "a", d -> edit(island(d), "(\"id\": \"v2\",\\s*\"type\": \"LOAD\",\\s*\"x\": )13", "$112"), "a load must be on a settlement");
expectError("diesel on settlement", "a", d -> edit(island(d), "(\"id\": \"d1\",\\s*\"type\": \"DIESEL\",\\s*\"x\": )11(,\\s*\"y\": )10", "$17$28"), "[d1]: the diesel plant must be on land");
expectError("two elements on one cell", "a", d -> edit(island(d), "(\"id\": \"s2\",\\s*\"x\": )10(,\\s*\"y\": )6", "$19$25"), "is already used by s1");
expectError("duplicate id", "a", d -> edit(island(d), "\"id\": \"s6\"", "\"id\": \"s5\""), "[s5]: id is used by 2");
// ---- 6. diesel count ----
expectError("two diesel plants", "a", d -> edit(island(d), "\"units\": \\[",
        "\"units\": [{\"id\":\"d2\",\"type\":\"DIESEL\",\"x\":4,\"y\":4,\"pmin\":1,\"pmax\":2,\"fuel\":0,\"co2\":0,\"start\":0},"), "exactly one diesel plant, found 2");
expectError("no diesel plant", "a", d -> edit(island(d), "\"type\": \"DIESEL\"", "\"type\": \"LOAD\", \"category\": \"village\", \"scale\": 1"), "found 0");
// ---- 7. allowed ----
expectError("empty allowed", "a", d -> edit(island(d), "\"allowed\": \\[\\s*\"WIND\"\\s*\\]", "\"allowed\": []"), "[s1]: site allows no technology");
expectError("unknown allowed", "a", d -> edit(island(d), "\"allowed\": \\[\\s*\"WIND\"\\s*\\]", "\"allowed\": [\"SOLAR\"]"), "unknown technology \"SOLAR\"");
// ---- 8. value ranges ----
expectError("pmin > pmax", "a", d -> edit(island(d), "\"pmin\": 100", "\"pmin\": 800"), "pmin must be between 0 and pmax");
expectError("negative budget", "a", d -> edit(island(d), "\"budget\": 900000", "\"budget\": -1"), "budget must be >= 0");
expectError("load scale 0", "a", d -> edit(island(d), "\"scale\": 0.35", "\"scale\": 0"), "scale must be > 0");
expectError("soc_min >= soc_max", "a", d -> edit(catalog(d), "\"soc_min\": 0.1", "\"soc_min\": 0.99"), "0 <= soc_min < soc_max <= 1");
expectError("eff > 1", "a", d -> edit(catalog(d), "\"eff\": 0.9", "\"eff\": 1.2"), "0 < eff <= 1");
expectError("cutin >= rated", "a", d -> edit(catalog(d), "\"cutin\": 3.0", "\"cutin\": 12.0"), "0 <= cutin < rated < cutout");
expectError("life 0", "a", d -> edit(catalog(d), "\"life\": 25", "\"life\": 0"), "[pv100]: life must be > 0");
// ---- 9. catalog ids / types ----
expectError("duplicate catalog id", "a", d -> edit(catalog(d), "\"id\": \"pv200\"", "\"id\": \"pv100\""), "[pv100]: duplicate catalog id");
expectError("unknown item type", "a", d -> edit(catalog(d), "\"type\": \"PV\"", "\"type\": \"SOLAR\""), "unknown item type 'SOLAR'");
expectError("unknown unit type", "a", d -> edit(island(d), "\"type\": \"LOAD\"", "\"type\": \"SHOP\""), "unknown unit type 'SHOP'");
// ---- 10. CSV ----
expectError("CSV with 23 hours", "a", d -> edit(day(d, "winter.csv"), "\n23,[^\n]*\n?$", "\n"), "has 23 data rows, expected 24");
expectError("CSV hours out of order", "a", d -> edit(day(d, "winter.csv"), "\n5,0.000", "\n7,0.000"), "hour is 7, expected 5");
expectError("CSV non-numeric value", "a", d -> edit(day(d, "winter.csv"), "0\\.605", "abc"), "'abc' is not a number");
expectError("CSV decimal comma", "a", d -> edit(day(d, "winter.csv"), "\n0,0.000,11.3,0.330", "\n0,0.000,11.3,0,330"), "[line 2]: has 5 values, expected 4");
expectError("CSV missing category column", "b", d -> edit(day(d, "summer.csv"), "hotel", "resort"), "days/summer.csv [h1]: no column 'hotel'");
expectError("CSV missing wind_ms", "a", d -> edit(day(d, "winter.csv"), "wind_ms", "wind"), "missing column 'wind_ms'");
expectError("CSV solar > 1", "a", d -> edit(day(d, "winter.csv"), "0\\.356", "1.356"), "solar must be between 0 and 1");
// ---- 11. weights ----
expectError("weights sum 364", "a", d -> edit(weights(d), "\"winter.csv\": 90", "\"winter.csv\": 89"), "weights add up to 364, expected 365");
expectError("weight 0", "a", d -> edit(weights(d), "\"winter.csv\": 90", "\"winter.csv\": 0"), "[winter.csv]: weight must be a positive integer");
expectError("weight 1.5", "a", d -> edit(weights(d), "\"winter.csv\": 90", "\"winter.csv\": 1.5"), "weight must be a positive integer, got 1.5");
expectError("weights lists a missing CSV", "a", d -> Files.delete(day(d, "spring.csv")), "days/spring.csv: file is missing");
expectError("weights path outside days/", "a", d -> edit(weights(d), "\"winter.csv\"", "\"../island.json\""), "directly inside the days/ folder");
// ---- many errors at once ----
expectError("errors from several files reported together", "a", d -> {
    edit(island(d), "\"pmin\": 100", "\"pmin\": 800");
    edit(catalog(d), "\"eff\": 0.9", "\"eff\": 1.2");
}, "catalog.json [b300]");

System.out.println();
System.out.println(passed + " passed, " + failed + " failed");
/exit (failed == 0 ? 0 : 1)
