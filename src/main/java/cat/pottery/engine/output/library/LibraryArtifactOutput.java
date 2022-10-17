package cat.pottery.engine.output.library;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LibraryArtifactOutput implements ArtifactOutput {
    @Override
    public void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath) {
        var outputFile = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + ".jar-tmp");
        var outputFileFinal = outputPath.resolve(artifactDocument.artifact().id() + "-" + artifactDocument.artifact().version() + ".jar");

        ZipOutputStream zos = null;
        var prefixToDelete = compilerOutput.toString() + "/";

        try {
            Files.createDirectories(outputFile.getParent());
            Files.createDirectories(compilerOutput.getParent());
            Files.deleteIfExists(outputFileFinal);
            zos = new ZipOutputStream(new FileOutputStream(outputFile.toFile()));

            ZipOutputStream out = zos;
            Files.walkFileTree(compilerOutput, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toFile().isFile()) {
                        return FileVisitResult.CONTINUE;
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

            out.close();
            Files.move(outputFile, outputFileFinal);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
