package cat.pottery.engine.output.nativeImage;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.engine.output.fatJar.FatJarArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class NativeImageArtifactOutput implements ArtifactOutput {
    private final Toolchain toolchain;
    private final FatJarArtifactOutput fatJarOutput;

    public NativeImageArtifactOutput(Toolchain toolchain, FatJarArtifactOutput fatJarOutput) {
        this.toolchain = toolchain;
        this.fatJarOutput = fatJarOutput;
    }

    @Override
    public void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath) {
        fatJarOutput.generateArtifact(artifactDocument, compilerOutput, classPath, outputPath);
        Path fatJarPath = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + "-fat.jar");
        Path outputImage = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version());

        var cmd = new LinkedList<>(List.of(
                toolchain.graalvmNative().toString(),
                "-jar",
                fatJarPath.toString(),
                "--install-exit-handlers",
                "--no-fallback",
                "--enable-https",
                outputImage.toString()
        ));

        try {
            var process = Runtime.getRuntime().exec(cmd.toArray(String[]::new));
            process.waitFor();

            System.out.println(new String(process.getInputStream().readAllBytes()));
            System.err.println(new String(process.getErrorStream().readAllBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
