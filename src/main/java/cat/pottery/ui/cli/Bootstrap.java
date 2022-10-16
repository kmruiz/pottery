package cat.pottery.ui.cli;

import cat.pottery.engine.compiler.IncrementalCompiler;
import cat.pottery.engine.dependencies.DependencyResolver;
import cat.pottery.engine.dependencies.DownloadManager;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.engine.output.fatJar.FatJarArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public final class Bootstrap {
    public static void main(String[] args) {
        var queue = new ArrayBlockingQueue<MavenDependency>(128);
        var manager = new DownloadManager(queue, new HashMap<>(), 4);

        var dependencyResolver = new DependencyResolver(manager, queue, 4);
        var artifactDoc = (ArtifactFileParserResult.Success) new YamlArtifactFileParser().parse(Path.of("pottery.yaml"));

        var deps = dependencyResolver.downloadDependenciesOfArtifact(artifactDoc.document());
        var compiler = new IncrementalCompiler(Toolchain.systemDefault());
        compiler.compileTree(Path.of("src", "main", "java").toAbsolutePath(), Path.of("target", "classes").toAbsolutePath(), deps);

        var artifactOutput = new FatJarArtifactOutput();
        artifactOutput.generateArtifact(
                artifactDoc.document(),
                Path.of("target", "classes"),
                deps,
                Path.of("target")
        );
    }
}
