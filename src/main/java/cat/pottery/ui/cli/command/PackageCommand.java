/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.cli.command;

import cat.pottery.engine.compiler.IncrementalCompiler;
import cat.pottery.engine.dependencies.DependencyResolver;
import cat.pottery.engine.dependencies.DownloadManager;
import cat.pottery.engine.dependencies.PomContextRegistry;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.engine.output.ArtifactOutput;
import cat.pottery.engine.output.container.ContainerArtifactOutput;
import cat.pottery.engine.output.fatJar.FatJarArtifactOutput;
import cat.pottery.engine.output.library.LibraryArtifactOutput;
import cat.pottery.engine.output.nativeImage.NativeImageArtifactOutput;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public final class PackageCommand implements CliCommand {

    private static final String TIMING_ID = "package-command";

    @Override
    public void execute(CommandLine.ParseResult parseResult) {
        Timing.getInstance().start(TIMING_ID);

        var queue = new ArrayBlockingQueue<MavenDependency>(128);
        var manager = new DownloadManager(queue, new ConcurrentHashMap<>(32, 1.2f, 4), 4, false);
        var pomContextRegistry = new PomContextRegistry(new ConcurrentHashMap<>(32, 1.2f, 4));

        var dependencyResolver = new DependencyResolver(manager, queue, 4, pomContextRegistry);
        ArtifactFileParserResult result = new YamlArtifactFileParser().parse(Path.of("pottery.yaml"));
        var artifactDoc = (ArtifactFileParserResult.Success) result;

        var deps = dependencyResolver.downloadDependenciesOfArtifact(artifactDoc.document());
        var compiler = new IncrementalCompiler(Toolchain.systemDefault());
        var process = compiler.compileTree(artifactDoc.document(), Path.of("src", "main", "java").toAbsolutePath(), Path.of("target", "classes").toAbsolutePath(), deps);

        if (process != null) {
            try {
                process.waitFor();
            } catch (InterruptedException e) {

            }
        }

        for (var produces : artifactDoc.document().artifact().platform().produces()) {
            ArtifactOutput artifactOutput = switch (produces.toLowerCase()) {
                case "fatjar" -> new FatJarArtifactOutput(Toolchain.systemDefault());
                case "native" -> new NativeImageArtifactOutput(Toolchain.systemDefault(), new FatJarArtifactOutput(Toolchain.systemDefault()));
                case "container" -> new ContainerArtifactOutput(Toolchain.systemDefault(), new FatJarArtifactOutput(Toolchain.systemDefault()));
                case "docker" -> new ContainerArtifactOutput(Toolchain.systemDefault(), new FatJarArtifactOutput(Toolchain.systemDefault()));
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


        var duration = Timing.getInstance().end(TIMING_ID);
        Log.getInstance().info("All packages built in %s.", duration);
    }
}
