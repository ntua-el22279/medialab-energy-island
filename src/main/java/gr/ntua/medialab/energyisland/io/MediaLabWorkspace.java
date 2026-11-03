package gr.ntua.medialab.energyisland.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@code medialab/} data folder: scenarios in {@code medialab/scenarios/}, saved plans
 * and results in {@code medialab/results/}.
 */
public final class MediaLabWorkspace {

    /** Name of the data folder, as required by the brief. */
    public static final String ROOT_NAME = "medialab";

    private final Path root;

    /**
     * Creates a workspace rooted at the given folder.
     *
     * @param root the {@code medialab} folder
     */
    public MediaLabWorkspace(Path root) {
        this.root = root;
    }

    /**
     * Returns the workspace in the current working directory, {@code ./medialab}.
     *
     * @return the default workspace
     */
    public static MediaLabWorkspace inWorkingDirectory() {
        return new MediaLabWorkspace(Path.of(ROOT_NAME));
    }

    /** @return the {@code medialab} folder */
    public Path root() {
        return root;
    }

    /** @return the {@code medialab/scenarios} folder */
    public Path scenariosDir() {
        return root.resolve("scenarios");
    }

    /** @return the {@code medialab/results} folder */
    public Path resultsDir() {
        return root.resolve("results");
    }

    /**
     * Returns the folder of a scenario.
     *
     * @param scenarioId the scenario id (folder name)
     * @return {@code medialab/scenarios/<id>}
     */
    public Path scenarioDir(String scenarioId) {
        return scenariosDir().resolve(scenarioId);
    }

    /**
     * Lists the available scenarios: every subfolder of {@code medialab/scenarios}.
     * They are not loaded or validated here.
     *
     * @return scenario ids, sorted by name; empty if the folder does not exist
     * @throws IOException if the folder cannot be read
     */
    public List<String> listScenarioIds() throws IOException {
        if (!Files.isDirectory(scenariosDir())) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(scenariosDir())) {
            return s.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
        }
    }
}
