# Validation and loading

Packages: `gr.ntua.medialab.energyisland.validation` and `gr.ntua.medialab.energyisland.io`.

## Flow

```
ScenarioLoader.load(medialab/scenarios/<id>/)
 ├─ IslandJson.read     island.json   → Optional<Island>   (fields, types, map shape/symbols, unit types, allowed names)
 ├─ CatalogJson.read    catalog.json  → Optional<Catalog>  (fields, types, item types, duplicate ids)
 ├─ DayFiles.read       days/weights.json + listed CSVs → List<DayProfile>
 ├─ IslandValidator.validate(island, days)   (placement, grid, ids, one diesel, ranges, load categories in every CSV)
 ├─ CatalogValidator.validate(catalog)       (ranges)
 └─ any issue → throw InvalidScenarioException(id, all issues)   else → Scenario
```

- **Collect everything, then fail.** Every reader appends `ValidationIssue(file, element, message)`
  to a shared list and carries on. The user sees all errors from all files at once.
- **Two layers of checks.**
  - *File-level* (`io`): anything the model cannot represent, such as missing or invalid JSON, a missing field,
    a wrong type, an unknown map symbol, an unknown unit/item/technology type, a duplicate catalog id, or a bad CSV/weight.
  - *Model-level* (`validation`): rules on an already-built `Island`/`Catalog`. The **editor** reuses
    these for live checking and highlighting, because `ValidationIssue.element` is the element id.
- **Strict JSON.** Gson's `JsonReader` with `Strictness.STRICT`, plus a check for trailing content.
  Comments, trailing commas, unquoted keys and `NaN` are rejected, and the error gives a line and column.
- **No Gson data binding.** Binding would silently turn a missing number into `0`. Using the tree API with
  `JsonFields` getters reports "missing required field" and "must be an integer/number/string/array".

## Guide §7 checks and where they are

| # | Check | Where |
|---|---|---|
| 1 | missing fixed file / invalid JSON | `JsonFiles.readText`, `JsonFiles.parse` |
| 2 | missing field / wrong type | `JsonFields` (used by all JSON readers) |
| 3 | map is not `height` rows × `width` chars, unknown symbol | `IslandJson.readMap` |
| 4 | coordinates off the grid | `IslandValidator.checkPlacement` |
| 5 | placement rules (§3.5), same cell, unique ids | `IslandValidator.checkPlacement`, `checkIds` |
| 6 | exactly one diesel plant | `IslandValidator.checkDieselCount` |
| 7 | empty / unknown `allowed` | `IslandValidator` (empty), `IslandJson.readSites` (unknown) |
| 8 | value ranges | `IslandValidator` (diesel, load, budget), `CatalogValidator` (items) |
| 9 | duplicate catalog id, unknown type | `CatalogJson.read`, `IslandJson.readUnits` |
| 10 | CSV: 24 consecutive hours, missing category column, non-numeric value | `DayFiles.readCsv`; missing category in `IslandValidator.checkLoad` |
| 11 | weights not positive integers / sum ≠ 365 / listed file missing | `DayFiles.read` |

## Assumptions (where the guide is silent)

- A UTF-8 BOM at the start of a file is allowed and ignored. `\r\n` line endings are accepted.
- Blank lines in CSV files are ignored. Line numbers in messages are physical file lines (header = line 1).
- CSV numbers must be plain decimals with a dot (`12`, `0.75`, `1e-3`). `NaN`, `Infinity`, hex and decimal
  commas are rejected. A decimal comma shows up as "has 5 values, expected 4".
- CSV range checks (beyond the guide's "non-numeric"): `solar` 0–1, `wind_ms` ≥ 0, profiles ≥ 0, as listed in guide §5.1.
- Columns for categories that no load uses are not read at all, so they may even contain non-numbers.
  If `island.json` itself failed to load, every extra column is checked.
- File names in `weights.json` must point directly inside `days/`. `../x.csv` is rejected.
- CSV files in `days/` that are not listed in `weights.json` are ignored (guide §5.2).
- Duplicate keys inside one JSON object are not detected; Gson keeps the last one.

## Regression check

`tools/check-validation.sh` compiles the project and runs `tools/validation-check.jsh` in jshell (no JUnit,
per the library rule). It loads the two samples, plus 4 valid variants (BOM, CRLF, unused column, unlisted
CSV). It also loads 44 copies of a sample, each with **one** fault, and checks that loading fails with the
expected message.

Result on 2026-11-03: **50 passed, 0 failed**.
