/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.output.fatJar;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class FatJarArtifactOutput implements ArtifactOutput {
    private static final String TIMING_ID = "generate-output";
    private final Toolchain toolchain;
    private static boolean wasBuiltAlready = false;


    public FatJarArtifactOutput(Toolchain toolchain) {
        this.toolchain = toolchain;
    }

    @Override
    public void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath) {
        if (wasBuiltAlready) {
            return;
        }

        Timing.getInstance().start(TIMING_ID);
        var outputFile = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + "-fat.jar-tmp");
        var outputFileFinal = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + "-fat.jar");

        ZipOutputStream zos = null;
        final boolean[] hasManifest = {false};
        var prefixToDelete = compilerOutput.toString() + "/";

        try {
            Files.createDirectories(outputFile.getParent());
            Files.createDirectories(compilerOutput.getParent());
            Files.deleteIfExists(outputFileFinal);
            zos = new ZipOutputStream(new FileOutputStream(outputFile.toFile()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ZipOutputStream out = zos;
        Set<String> addedEntries = new HashSet<>();

        var onlyJars = classPath.stream().filter(file -> file.downloadPath().getFileName().toString().endsWith(".jar")).toList();

        onlyJars.forEach(dependency -> {
            var pathToJar = dependency.downloadPath();

            try (var zip = new ZipFile(pathToJar.toFile())) {
                zip.stream().filter(e -> !e.getName().endsWith("META-INF/MANIFEST.MF")).forEach(depEntry -> {
                    if (addedEntries.contains(depEntry.getName())) {
                        return;
                    }

                    addedEntries.add(depEntry.getName());

                    ZipEntry output = new ZipEntry(depEntry.getName().replace(prefixToDelete, ""));
                    try {
                        out.putNextEntry(output);
                        try (var input = zip.getInputStream(depEntry)) {
                            input.transferTo(out);
                        }
                        out.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (IOException e) {
                throw new Error(e);
            }
        });
        // now add my own compiled path
        try {
            Files.walkFileTree(compilerOutput, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toFile().isFile()) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (file.endsWith("META-INF/MANIFEST.MF")) {
                        hasManifest[0] = true;
                    }
                    try (var input = new FileInputStream(file.toFile())) {
                        ZipEntry output = new ZipEntry(file.toString().replace(prefixToDelete, ""));
                        out.putNextEntry(output);
                        input.transferTo(out);
                        out.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
        // now add the manifest
        if (!hasManifest[0]) {
            ZipEntry manifest = new ZipEntry("META-INF/MANIFEST.MF");
            try {
                out.putNextEntry(manifest);
                out.write("""
                        Manifest-Version: 1.0
                        Created-By: cat.pottery %s
                        Built-By: %s
                        Build-Jdk: %s
                        Main-Class: %s
                        """.formatted(
                        toolchain.potteryVersion(),
                        toolchain.currentUser(),
                        toolchain.javacVersion(),
                        artifactDocument.artifact().manifest().mainClass()
                ).getBytes());
                out.closeEntry();
                out.close();
                Files.move(outputFile, outputFileFinal);
                var duration = Timing.getInstance().end(TIMING_ID);
                Log.getInstance().info("Built fatJar %s in %s.", outputFileFinal.toString(), duration);
                wasBuiltAlready = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                out.close();
                Files.move(outputFile, outputFileFinal);
                var duration = Timing.getInstance().end(TIMING_ID);
                Log.getInstance().info("Built fatJar %s in %s.", outputFileFinal.toString(), duration);
                wasBuiltAlready = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
