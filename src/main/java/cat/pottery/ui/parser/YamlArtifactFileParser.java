package cat.pottery.ui.parser;

import cat.pottery.ui.artifact.*;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class YamlArtifactFileParser implements ArtifactFileParser {
    private final Yaml yaml;

    public YamlArtifactFileParser() {
        this.yaml = new Yaml();
    }

    @Override
    public ArtifactFileParserResult parse(Path path) {
        Map<String, Object> dom;
        try {
            dom = yaml.load(Files.readString(path));
        } catch (IOException e) {
            return new ArtifactFileParserResult.Failure(List.of(new ArtifactFileParserResult.ErrorMessage("Could not parse YAML file at %s. Error message: %s".formatted(path.toString(), e.getMessage()))));
        }

        var paramMap = (Map<String, String>) dom.getOrDefault("parameters", Map.of());
        var artifact = (Map<String, Object>) dom.get("artifact");
        var platformProduceNormalisedList = new ArrayList<String>(5);

        if (artifact == null) {
            return new ArtifactFileParserResult.Failure(List.of(new ArtifactFileParserResult.ErrorMessage("Document does not contain an artifact section.")));
        }

        var artifactGroup = artifact.get("group").toString();
        var artifactId = artifact.get("id").toString();
        var version = artifact.get("version").toString();

        var platform = (Map<String, Object>) artifact.get("platform");
        var platformVersion = (String) platform.get("version");
        Object platformProduces = platform.getOrDefault("produces", Collections.emptyList());

        if (platformProduces instanceof String) {
            platformProduceNormalisedList.add(platformProduces.toString());
        } else if (platformProduces instanceof List) {
            platformProduceNormalisedList.addAll((Collection<? extends String>) platformProduces);
        }

        var manifest = (HashMap<String, String>) artifact.getOrDefault("manifest", new HashMap<>());
        var mainClass = manifest.getOrDefault("main-class", "").toString();

        var dependencies = (List<Map<String, String>>) artifact.getOrDefault("dependencies", Collections.emptyList());
        var parsedDependencies = new ArrayList<Dependency>(dependencies.size());

        for (var dep : dependencies) {
            if (dep.size() != 1) {
                return null;
            }

            if (dep.containsKey("production")) {
                parsedDependencies.add(new Dependency(Dependency.Scope.PRODUCTION, dep.get("production")));
            } else if (dep.containsKey("test")) {
                parsedDependencies.add(new Dependency(Dependency.Scope.TEST, dep.get("test")));
            }
        }

        return new ArtifactFileParserResult.Success(
                new ArtifactDocument(
                        paramMap,
                        new Artifact(
                                artifactGroup,
                                artifactId,
                                version,
                                new Platform(platformVersion, platformProduceNormalisedList),
                                parsedDependencies,
                                new Manifest(mainClass)
                        )
                ),
                Collections.emptyList()
        );
    }
}
