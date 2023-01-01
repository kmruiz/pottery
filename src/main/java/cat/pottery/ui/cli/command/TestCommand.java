package cat.pottery.ui.cli.command;

import cat.pottery.telemetry.Log;
import picocli.CommandLine;

public class TestCommand implements CliCommand {
    @Override
    public void execute(CommandLine.ParseResult parseResult) {
        Log.getInstance().error("Test not implemented");
    }
}
