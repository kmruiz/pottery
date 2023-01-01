package cat.pottery.ui.cli.command;

import picocli.CommandLine;

public interface CliCommand {
    void execute(CommandLine.ParseResult parseResult);
}
