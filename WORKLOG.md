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
├── pom.xml                     ← Maven build (JavaFX 25.0.5, Gson 2.15.0, Java 25)
├── docs/
│   └── course-material/        ← brief PDF + scenario guide (read-only reference)
├── src/main/
│   ├── java/gr/ntua/medialab/energyisland/
│   │   ├── App.java            ← JavaFX entry point (window "MediaLab Energy Island")
│   │   ├── model/              ← grid, terrain, units, catalog, plans
│   │   ├── simulation/         ← hourly simulation + controller
│   │   ├── optimizer/          ← exhaustive (with pruning) + heuristic search
│   │   ├── io/                 ← scenario loading/validation, plan/result JSON (Gson)
│   │   └── ui/                 ← JavaFX views
│   └── resources/              ← icons, CSS (empty for now)
└── medialab/                   ← required app data folder (per brief, A.2)
    ├── scenarios/
    │   ├── a/                  ← sample scenario (4 days)
    │   └── b/                  ← sample scenario (6 days, incl. windless, summer_peak)
    └── results/                ← saved plans & optimisation results (app output)
```

### How to build and run

```bash
mvn compile        # compile
mvn javafx:run     # run the app (from the project root, so medialab/ is found)
mvn package        # build target/energy-island-0.1.0-SNAPSHOT.jar
```

## Environment

| Tool | Status (2026-11-03) |
|---|---|
| OS | Linux (kernel 7.0.0) |
| Java runtime | OpenJDK 25.0.4.1 ✅ |
| `javac` (JDK) | ✅ 25.0.4.1 (`openjdk-25-jdk`, `/usr/lib/jvm/java-25-openjdk-amd64`) |
| Maven | ✅ 3.9.12 (`/usr/share/maven`, uses JDK 25) |
| JavaFX | ✅ 25.0.5 through Maven (`org.openjfx:javafx-controls`) |
| Git | 2.53.0 ✅ |
| Editor | VS Code 1.142.0 ✅ |

## Open items / decisions pending

- [x] Install a full JDK: `openjdk-25-jdk` (2026-11-03)
- [x] Build tool: **Maven** 3.9.12 (2026-11-03); JavaFX will come through Maven
- [x] JSON library: **Gson** (`com.google.code.gson:gson:2.15.0`) (2026-11-03)
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

### 2026-11-03: JDK installed

**Done**
- The user ran `sudo apt install openjdk-25-jdk`.
- Checked: `javac 25.0.4.1` and `java 25.0.4.1`, at `/usr/lib/jvm/java-25-openjdk-amd64`.
- Updated the *Environment* table and *Open items*.

**Next**
- Choose and install a build tool (Maven or Gradle) and choose the JSON library.

### 2026-11-03: Maven installed

**Done**
- The user ran `sudo apt install maven`.
- Checked: Apache Maven 3.9.12 at `/usr/share/maven`, running on JDK 25.0.4.1.
- Decided JavaFX will be a Maven dependency (no system-wide SDK needed).

**Next**
- Choose the JSON library (suggested: Gson).

### 2026-11-03: JSON library chosen

**Done**
- Decided: **Gson** is the single allowed JSON library (small, maps JSON straight to Java classes).
- Looked up current versions on Maven Central:
  - Gson: latest release `2.15.0`
  - JavaFX: latest stable `27.0.1`; latest `25.x` is `25.0.5`
- Planned dependencies for the future `pom.xml`:
  - `com.google.code.gson:gson:2.15.0`
  - `org.openjfx:javafx-controls:25.0.5`, chosen to match the installed JDK 25, since newer JavaFX lines may need a newer JDK

**Status**
- Workspace and toolchain are ready. Next step: create the Maven project (not started yet).

### 2026-11-03: Maven project created

**Done**
- Created `pom.xml`:
  - coordinates `gr.ntua.medialab:energy-island:0.1.0-SNAPSHOT`, Java release 25, UTF-8
  - dependencies: `javafx-controls 25.0.5` and `gson 2.15.0` (nothing else, per the brief)
  - plugins: `maven-compiler-plugin 3.16.0`, `maven-jar-plugin 3.5.1`, `javafx-maven-plugin 0.0.8` (main class `gr.ntua.medialab.energyisland.App`)
- Created the base package `gr.ntua.medialab.energyisland` with:
  - `App.java`: a minimal JavaFX window titled "MediaLab Energy Island", 1280×800, holding a placeholder label
  - sub-packages `model`, `simulation`, `optimizer`, `io`, `ui`, each with a `package-info.java` describing its role
- Created an empty `src/main/resources/`.

**Checked**
- `mvn compile` succeeds.
- `mvn javafx:run` starts the window with no errors (stopped after 12 s).

**Decisions / notes**
- No JUnit or other test library added, because the brief allows only Java SE + JavaFX + one JSON library. Tests, if any, will use plain Java.
- Not using `module-info.java` (classpath app) to avoid Gson reflection/`opens` complications. Can revisit.
- The `sun.misc.Unsafe` warnings during builds come from Maven's own bundled Guava and are harmless.

**Next**
- Design the model classes from the scenario guide (`island.json`, `catalog.json`, `days/`).
