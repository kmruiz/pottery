package cat.pottery.ui.cli;

import cat.pottery.ui.cli.command.CommandResolver;
import picocli.CommandLine;

import static cat.pottery.ui.cli.command.CommandResolver.CMD_SPEC;

public final class Bootstrap {
    public static void main(String[] args) {
        if (args.length < 1) {
            return;
        }

        var cmdLine = new CommandLine(CMD_SPEC);
        var parseResult = cmdLine.parseArgs(args);
        if (parseResult.isUsageHelpRequested()) {
            cmdLine.usage(cmdLine.getOut());
            System.exit(cmdLine.getCommandSpec().exitCodeOnUsageHelp());
        } else if (parseResult.isVersionHelpRequested()) {
            cmdLine.printVersionHelp(cmdLine.getOut());
            System.exit(cmdLine.getCommandSpec().exitCodeOnVersionHelp());
        }

        if (!parseResult.hasSubcommand()) {
            cmdLine.usage(cmdLine.getOut());
            System.exit(cmdLine.getCommandSpec().exitCodeOnUsageHelp());
        }

        var clicmd = new CommandResolver().byName(parseResult.subcommand().commandSpec().name());
        clicmd.execute(parseResult.subcommand());
    }
}
