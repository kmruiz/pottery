/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.cli;

import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
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

        if (parseResult.subcommand().isUsageHelpRequested()) {
            var innerCmdLine = new CommandLine(parseResult.subcommand().commandSpec());
            innerCmdLine.usage(innerCmdLine.getOut());
            System.exit(innerCmdLine.getCommandSpec().exitCodeOnUsageHelp());
        }

        Log.getInstance().info("Running pottery version %s", Toolchain.systemDefault().potteryVersion());
        var clicmd = new CommandResolver().byName(parseResult.subcommand().commandSpec().name());
        clicmd.execute(parseResult.subcommand());
    }
}
