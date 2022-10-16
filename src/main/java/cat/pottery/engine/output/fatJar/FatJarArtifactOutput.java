package cat.pottery.engine.output.fatJar;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class FatJarArtifactOutput implements ArtifactOutput {
    @Override
    public void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath) {
        var outputFile = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + "-fat.jar-tmp");
        var outputFileFinal = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + "-fat.jar");
        ZipOutputStream zos = null;
        final boolean[] hasManifest = {false};
        var prefixToDelete = compilerOutput.toString() + "/";

        try {
            Files.createDirectories(outputFile.getParent());
            Files.deleteIfExists(outputFileFinal);
            zos = new ZipOutputStream(new FileOutputStream(outputFile.toFile()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ZipOutputStream out = zos;
        classPath.forEach(dependency -> {
            var pathToJar = dependency.downloadPath();
            try (var zip = new ZipFile(pathToJar.toFile())) {
                zip.stream().filter(e -> !e.getName().endsWith("META-INF/MANIFEST.MF")).forEach(depEntry -> {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // now add the manifest
        if (!hasManifest[0]) {
            ZipEntry manifest = new ZipEntry("META-INF/MANIFEST.MF");
            try {
                out.putNextEntry(manifest);
                out.write("""
                    Manifest-Version: 1.0
                    Created-By: Apache Maven 3.8.1
                    Built-By: kevin
                    Build-Jdk: 18.0.1
                    Class-Path: snakeyaml-1.33.jar
                    Main-Class: cat.pottery.ui.cli.Bootstrap
                    """.getBytes());
                out.closeEntry();
                out.close();
                Files.move(outputFile, outputFileFinal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                out.close();
                Files.move(outputFile, outputFileFinal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
