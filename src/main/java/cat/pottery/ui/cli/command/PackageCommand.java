package cat.pottery.ui.cli.command;

import cat.pottery.engine.compiler.IncrementalCompiler;
import cat.pottery.engine.dependencies.DependencyResolver;
import cat.pottery.engine.dependencies.DownloadManager;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.engine.output.container.ContainerArtifactOutput;
import cat.pottery.engine.output.fatJar.FatJarArtifactOutput;
import cat.pottery.engine.output.library.LibraryArtifactOutput;
import cat.pottery.engine.output.nativeImage.NativeImageArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public final class PackageCommand implements CliCommand {
    @Override
    public void execute(String[] args) {
        var queue = new ArrayBlockingQueue<MavenDependency>(128);
        var manager = new DownloadManager(queue, new HashMap<>(), 4);

        var dependencyResolver = new DependencyResolver(manager, queue, 4);
        var artifactDoc = (ArtifactFileParserResult.Success) new YamlArtifactFileParser().parse(Path.of("pottery.yaml"));

        var deps = dependencyResolver.downloadDependenciesOfArtifact(artifactDoc.document());
        var compiler = new IncrementalCompiler(Toolchain.systemDefault());
        var process = compiler.compileTree(artifactDoc.document(), Path.of("src", "main", "java").toAbsolutePath(), Path.of("target", "classes").toAbsolutePath(), deps);

        if (process != null) {
            try {
                process.waitFor();
            } catch (InterruptedException e) {

            }
        }

        ArtifactOutput artifactOutput = switch (artifactDoc.document().artifact().platform().produces().toLowerCase()) {
            case "fatjar" -> new FatJarArtifactOutput(Toolchain.systemDefault());
            case "native" -> new NativeImageArtifactOutput(Toolchain.systemDefault(), new FatJarArtifactOutput(Toolchain.systemDefault()));
            case "container" -> new ContainerArtifactOutput(Toolchain.systemDefault(), new FatJarArtifactOutput(Toolchain.systemDefault()));
            case "library" -> new LibraryArtifactOutput();
            default -> throw new Error();
        };

        artifactOutput.generateArtifact(
                artifactDoc.document(),
                Path.of("target", "classes"),
                deps,
                Path.of("target")
        );
    }
}
