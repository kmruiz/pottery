package cat.pottery.ui.cli;

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
import cat.pottery.ui.cli.command.CliCommand;
import cat.pottery.ui.cli.command.CommandResolver;
import cat.pottery.ui.cli.command.WatchCommand;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public final class Bootstrap {
    public static void main(String[] args) {
        if (args.length < 1) {
            return;
        }

        CliCommand cmd = new CommandResolver().byName(args[0]);
        cmd.execute(args);

    }
}
