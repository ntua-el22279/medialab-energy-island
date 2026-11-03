package gr.ntua.medialab.energyisland.io;

import gr.ntua.medialab.energyisland.model.Catalog;
import gr.ntua.medialab.energyisland.model.DayProfile;
import gr.ntua.medialab.energyisland.model.Island;
import gr.ntua.medialab.energyisland.model.Load;
import gr.ntua.medialab.energyisland.model.Scenario;
import gr.ntua.medialab.energyisland.validation.CatalogValidator;
import gr.ntua.medialab.energyisland.validation.IslandValidator;
import gr.ntua.medialab.energyisland.validation.ValidationIssue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads a scenario folder ({@code island.json}, {@code catalog.json}, {@code days/}) and
 * checks every rule of the scenario guide (section 7). All problems in all files are
 * collected and reported together; a scenario with any error is not loaded.
 */
public final class ScenarioLoader {

    /**
     * Loads and validates a scenario.
     *
     * @param scenarioDir the scenario folder; its name becomes the scenario id
     * @return the loaded scenario
     * @throws InvalidScenarioException if any file is missing or invalid; it lists every problem
     */
    public Scenario load(Path scenarioDir) throws InvalidScenarioException {
        String id = scenarioDir.getFileName().toString();
        List<ValidationIssue> issues = new ArrayList<>();

        Optional<Island> island = IslandJson.read(scenarioDir.resolve(IslandJson.FILE), issues);
        Optional<Catalog> catalog = CatalogJson.read(scenarioDir.resolve(CatalogJson.FILE), issues);
        Set<String> categories = island.map(i -> i.loads().stream().map(Load::category)
                .collect(Collectors.toSet())).orElse(null);
        List<DayProfile> days = DayFiles.read(scenarioDir.resolve(DayFiles.DIR), categories, issues);

        island.ifPresent(i -> issues.addAll(IslandValidator.validate(i, days)));
        catalog.ifPresent(c -> issues.addAll(CatalogValidator.validate(c)));

        if (!issues.isEmpty() || island.isEmpty() || catalog.isEmpty()) {
            throw new InvalidScenarioException(id, issues);
        }
        return new Scenario(id, island.get(), catalog.get(), days);
    }
}
