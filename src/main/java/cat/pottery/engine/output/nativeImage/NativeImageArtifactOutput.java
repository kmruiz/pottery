/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.output.nativeImage;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.engine.output.fatJar.FatJarArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class NativeImageArtifactOutput implements ArtifactOutput {
    private static final String TIMING_ID = "generate-output-native-image";
    private final Toolchain toolchain;
    private final FatJarArtifactOutput fatJarOutput;

    public NativeImageArtifactOutput(Toolchain toolchain, FatJarArtifactOutput fatJarOutput) {
        this.toolchain = toolchain;
        this.fatJarOutput = fatJarOutput;
    }

    @Override
    public void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath) {
        Log.getInstance().info("Generating required fatJar for native image.");
        fatJarOutput.generateArtifact(artifactDocument, compilerOutput, classPath, outputPath);

        Timing.getInstance().start(TIMING_ID);
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

            var duration = Timing.getInstance().end(TIMING_ID);

//            System.out.println(new String(process.getInputStream().readAllBytes()));
//            System.err.println(new String(process.getErrorStream().readAllBytes()));
            Log.getInstance().info("Built GraalVM native image %s in %s.", outputImage.toString(), duration);
        } catch (Exception e) {
            Log.getInstance().fatal("Error generating native image.", e);
            throw new RuntimeException(e);
        }
    }
}
