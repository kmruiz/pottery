package cat.pottery.engine.output.container;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.engine.output.fatJar.FatJarArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ContainerArtifactOutput implements ArtifactOutput {
    private static final String TIMING_ID = "generate-output";
    private final Toolchain toolchain;
    private final FatJarArtifactOutput fatJarOutput;

    public ContainerArtifactOutput(Toolchain toolchain, FatJarArtifactOutput fatJarOutput) {
        this.toolchain = toolchain;
        this.fatJarOutput = fatJarOutput;
    }

    @Override
    public void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath) {
        fatJarOutput.generateArtifact(artifactDocument, compilerOutput, classPath, outputPath);
        Path fatJarPath = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + "-fat.jar");

        String declaration = """
                FROM docker.io/openjdk:%s
                LABEL version="%s" buildat="%s"
                COPY %s /app/application.jar
                WORKDIR /app/
                ENTRYPOINT java -jar /app/application.jar
                """.formatted(
                        artifactDocument.artifact().platform().version(),
                artifactDocument.artifact().version(),
                new Date().toString(),
                fatJarPath.toString()
        );

        Path outputContainerfile = outputPath.resolve("Containerfile");
        try {
            Files.writeString(outputContainerfile, declaration);

            String containerName = "%s/%s:%s".formatted(artifactDocument.artifact().group(), artifactDocument.artifact().id(), artifactDocument.artifact().version());
            var cmd = new LinkedList<>(List.of(
                    toolchain.containerBuilder().toString(),
                    "build",
                    "-f",
                    outputContainerfile.toString(),
                    "-t",
                    containerName,
                    "."
            ));

            Timing.getInstance().start(TIMING_ID);
            var process = Runtime.getRuntime().exec(cmd.toArray(String[]::new));
            process.waitFor();
            Timing.getInstance().end(TIMING_ID);
            var duration = Timing.getInstance().durationOf(TIMING_ID);
            Log.getInstance().info("Built docker container %s in %dms.", containerName, duration.toMillis());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
