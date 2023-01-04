package cat.pottery.ui.cli.command;

import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class PurgeCommand implements CliCommand {
    private static final String TIMING_ID = "package-command";

    public void execute(CommandLine.ParseResult parseResult) {
        Timing.getInstance().start(TIMING_ID);

        try {
            Files.walk(Path.of(".pottery"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.walk(Path.of("target"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            Log.getInstance().error("Could not purge the project.", e);
            throw new RuntimeException(e);
        }

        var duration = Timing.getInstance().end(TIMING_ID);
        Log.getInstance().info("Project purged in %s.", duration);
    }
}
