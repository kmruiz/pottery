package cat.pottery.ui.cli.command;

import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.artifact.Artifact;
import cat.pottery.ui.artifact.ArtifactDocument;
import cat.pottery.ui.artifact.Manifest;
import cat.pottery.ui.artifact.Platform;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine;

import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public class InitCommand implements CliCommand {
    private static final String TIMING_ID = "init-command";

    @Override
    public void execute(CommandLine.ParseResult parseResult) {
        Timing.getInstance().start(TIMING_ID);
        var directory = parseResult.matchedPositionalValue(0, ".");
        var groupId = parseResult.matchedPositionalValue(1, directory);
        groupId = groupId.equals(".") ? "" : groupId;

        var artifactId = parseResult.matchedPositionalValue(2, "");
        var version = parseResult.matchedPositionalValue(3, "0.0.1");
        var jdk = parseResult.matchedOptionValue('j', "17");
        var produces = Arrays.stream(parseResult.matchedOptionValue('p', "fatjar").split(",")).toList();

        var parentDirectory = Path.of(directory);
        var javaDirectory = parentDirectory.resolve("src").resolve("main").resolve("java");

        var artifact = new ArtifactDocument(Collections.emptyMap(),
                new Artifact(
                        groupId,
                        artifactId,
                        version,
                        new Platform(jdk, produces),
                        Collections.emptyList(),
                        new Manifest("Main")
                )
        );

        try {
            Files.createDirectories(parentDirectory);
            Files.createDirectories(javaDirectory);

            PropertyUtils propUtils = new PropertyUtils();
            propUtils.setAllowReadOnlyProperties(true);
            Representer repr = new Representer();
            repr.addClassTag(ArtifactDocument.class, Tag.MAP);
            repr.setPropertyUtils(propUtils);
            TypeDescription manifestDesc = new TypeDescription(Manifest.class, Tag.MAP);
            manifestDesc.substituteProperty("main-class", String.class,"getMainClass", "setMainClass");
            manifestDesc.setExcludes("mainClass");
            Constructor constructor = new Constructor();
            constructor.addTypeDescription(manifestDesc);
            repr.addTypeDescription(manifestDesc);

            Yaml yaml = new Yaml(constructor, repr);
            var potteryYaml = yaml.dump(artifact);
            Files.writeString(parentDirectory.resolve("pottery.yaml"), potteryYaml);
            Files.writeString(javaDirectory.resolve("Main.java"),
                    """
                            public class Main {
                                public static void main(String[] args) {
                                    System.out.println("Hello world from Pottery!");
                                }
                            }
                            """
                    );
        } catch (Throwable ex) {
            Log.getInstance().error("Could not initialise project.", ex);
            System.exit(1);
        } finally {
            var duration = Timing.getInstance().end(TIMING_ID);
            Log.getInstance().info("Task ran in %s.", duration);
        }
    }
}
