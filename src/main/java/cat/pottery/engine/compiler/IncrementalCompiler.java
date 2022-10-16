package cat.pottery.engine.compiler;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.toolchain.Toolchain;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class IncrementalCompiler {
    private final Toolchain toolchain;

    public IncrementalCompiler(Toolchain toolchain) {
        this.toolchain = toolchain;
    }

    private record CompilationUnit(Path javaClass, Path classFile) {}

    public void compileTree(Path sourceCode, Path targetDirectory, List<DownloadedDependency> dependencies) {
        var classpath = dependencies.stream().map(e -> e.downloadPath().toString()).collect(Collectors.joining(":")) + ":" + targetDirectory.toString();
        var filesToCompile = new LinkedList<CompilationUnit>();

        try {
            Files.walkFileTree(sourceCode, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        // check if needs to be compiled
                        var relativeToSource = file.toString().replace(sourceCode + "/", "");
                        var classFileName = relativeToSource.replace(".java", ".class");
                        var classFile = targetDirectory.resolve(classFileName);

                        if (!classFile.toFile().exists()) {
                            filesToCompile.add(new CompilationUnit(file, classFile));
                        } else {
                            try {
                                var classFileAttr = Files.readAttributes(classFile, BasicFileAttributes.class);
                                if (attrs.lastModifiedTime().compareTo(classFileAttr.lastModifiedTime()) >= 1 ||
                                        attrs.lastModifiedTime().compareTo(classFileAttr.creationTime()) >= 1) {
                                    filesToCompile.add(new CompilationUnit(file, classFile));
                                }
                            } catch (IOException e) {
                                filesToCompile.add(new CompilationUnit(file, classFile));
                            }
                        }
                    } else {
                        var relativeToSource = file.toString().replace(sourceCode + "/", "");
                        var classFileName = relativeToSource.replace(".java", ".class");
                        var classFile = targetDirectory.resolve(classFileName);

                        if (!classFile.toFile().exists()) { // it's a resource, just copy it
                            try {
                                Files.copy(file, classFile);
                            } catch (IOException e) {
                                throw new Error(e);
                            }
                        } else {
                            try {
                                var classFileAttr = Files.readAttributes(classFile, BasicFileAttributes.class);
                                if (attrs.lastModifiedTime().compareTo(classFileAttr.lastModifiedTime()) >= 1 ||
                                        attrs.lastModifiedTime().compareTo(classFileAttr.creationTime()) >= 1) {
                                    Files.copy(file, classFile);
                                }
                            } catch (IOException e) {
                                try {
                                    Files.copy(file, classFile);
                                } catch (IOException ex) {
                                    throw new Error(ex);
                                }
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new Error(e);
        }

        if (filesToCompile.isEmpty()) {
            return;
        }

        var output = targetDirectory.toString();

        var cmd = new LinkedList<>(List.of(
                toolchain.javac().toString(),
                "-cp",
                classpath,
                "-d",
                output
        ));
        cmd.addAll(filesToCompile.stream().map(e -> e.javaClass.toString()).toList());

        try {
            Runtime.getRuntime().exec(cmd.toArray(String[]::new));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
