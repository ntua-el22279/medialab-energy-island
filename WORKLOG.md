# MediaLab Energy Island — Work Log

Running record of everything done and created for the final project of the course
**Τεχνολογία Πολυμέσων** (Multimedia Technology), NTUA ECE, 7th semester, 2026–27.

Newest entries go at the bottom of the [Log](#log). Each entry: date, what was done, files touched, decisions, open items.

---

## Project at a glance

A **JavaFX** desktop tool for designing the energy system of an off-grid island
(today powered only by a diesel plant). It answers: *what to build (PV, wind, batteries)
and where, so the island has power all year at the lowest annual cost, within budget.*

| Part | Weight | Content |
|---|---|---|
| A.1 Logic | 40% | Island grid model, hourly simulation with fixed controller, optimiser (exhaustive + pruning, heuristic) |
| A.2 Storage | 10% | JSON scenario loading + validation, own schema for plans/results in `medialab/results/` |
| A.3 GUI | 30% | JavaFX window "MediaLab Energy Island": map, editor, simulation playback, optimisation progress, top-10 results, menus |
| A.4 Other | 20% | OOP design, Javadoc on every public method of one class, only Java SE + JavaFX + one JSON library |

Full brief: `docs/course-material/multimedia_project_26-27_v1.pdf`
Scenario file format (binding): `docs/course-material/ΟΔΗΓΟΣ_ΣΕΝΑΡΙΩΝ.md`

## Workspace layout

```
energy-island/
├── WORKLOG.md                  ← this file
├── CLAUDE.md                   ← instructions for Claude Code sessions
├── .gitignore
├── docs/
│   └── course-material/        ← brief PDF + scenario guide (read-only reference)
└── medialab/                   ← required app data folder (per brief, A.2)
    ├── scenarios/
    │   ├── a/                  ← sample scenario (4 days)
    │   └── b/                  ← sample scenario (6 days, incl. windless, summer_peak)
    └── results/                ← saved plans & optimisation results (app output)
```

Source code is not created yet.

## Environment

| Tool | Status (2026-11-03) |
|---|---|
| OS | Linux (kernel 7.0.0) |
| Java runtime | OpenJDK 25.0.4.1 ✅ |
| `javac` (JDK) | ❌ not found: only a JRE is installed |
| Maven / Gradle | ❌ not found |
| JavaFX SDK | ❌ not found |
| Git | 2.53.0 ✅ |
| Editor | VS Code 1.142.0 ✅ |

## Open items / decisions pending

- [ ] Install a full JDK (e.g. `sudo apt install openjdk-25-jdk` or `openjdk-21-jdk`)
- [ ] Choose build tool (Maven or Gradle), which also pulls JavaFX as a dependency
- [ ] Choose the one allowed JSON library (e.g. Gson, Jackson, org.json)
- [ ] Define the JSON schema for saved plans/results in `medialab/results/`
- [ ] Decide heuristic optimisation method (e.g. greedy + local search)

---

## Log

### 2026-11-03: Workspace setup

**Done**
- Read the brief and the scenario guide, and summarised the project goals (see *Project at a glance*).
- Checked `medialab_scenarios.zip` against the already extracted `medialab/` folder: identical.
- Created the workspace `~/Projects/energy-island/`:
  - copied the brief PDF and the scenario guide into `docs/course-material/`
  - copied the sample scenarios `a`, `b` into `medialab/scenarios/`
  - created `medialab/results/` (with `.gitkeep`)
- Added `WORKLOG.md`, `CLAUDE.md` and `.gitignore` (Java/Maven/Gradle/IDE).
- Initialised a git repository.
- Checked the environment (see table above).

**Notes**
- The originals in `~/Projects/` (`medialab/`, the zip, the PDF) were left untouched.
- No source code created, by request.
