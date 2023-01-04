/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.cli.command;

import cat.pottery.engine.toolchain.Toolchain;
import picocli.CommandLine;

public final class CommandResolver {
    public CliCommand byName(String name) {
        return switch (name.trim().toLowerCase()) {
            case "watch" -> new WatchCommand();
            case "package" -> new PackageCommand();
            case "test" -> new TestCommand();
            case "idea" -> new IdeaCommand();
            case "init" -> new InitCommand();
            case "clean" -> new CleanCommand();
            case "purge" -> new PurgeCommand();
            default -> new PackageCommand();
        };
    }

    private static CommandLine.Model.CommandSpec PACKAGE_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Produces a package for each `produces` section"))
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());
    private static CommandLine.Model.CommandSpec IDEA_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Edits IDEA workspace files so the IDE detect dependencies downloaded by pottery."))
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());
    private static CommandLine.Model.CommandSpec CLEAN_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Deletes the `target` folder, which is the output of building a pottery package."))
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());
    private static CommandLine.Model.CommandSpec PURGE_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Deletes both the `.pottery` folder, which contains downloaded dependencies, and the `target` folder, which contains the compiled artifacts of a pottery package."))
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());
    private static CommandLine.Model.CommandSpec INIT_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Creates a new pottery project in the specified directory."))
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("0")
                    .paramLabel("DIRECTORY")
                    .type(String.class)
                    .description("Directory where to initialise the project")
                    .build())
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("1")
                    .paramLabel("GROUP_ID")
                    .type(String.class)
                    .description("Maven groupId of the artifact")
                    .build())
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("2")
                    .paramLabel("ARTIFACT_ID")
                    .type(String.class)
                    .description("Maven artifactId of the artifact")
                    .build())
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("3")
                    .paramLabel("VERSION")
                    .type(String.class)
                    .description("Version of the artifact")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("-j", "--jdk")
                    .type(String.class)
                    .paramLabel("JDK")
                    .description("Target JDK for the artifact.")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("-p", "--produces")
                    .type(String.class)
                    .paramLabel("PRODUCES")
                    .description("Comma-separated list of artifact types that the project produce. Options: fatjar, library, container, docker, native.")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());

    private static CommandLine.Model.CommandSpec TEST_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Runs jUnit 5 tests in the `src/test/java` folder and prints a summary of the results."))
            .addOption(CommandLine.Model.OptionSpec.builder("-v", "--verbose")
                    .description("Show stack traces for failed tests.")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("-x", "--strict")
                    .description("Strict mode. Forces failure when there are skipped tests.")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());

    private static final CommandLine.Model.CommandSpec WATCH_SPEC = CommandLine.Model.CommandSpec.create()
            .usageMessage(new CommandLine.Model.UsageMessageSpec()
                    .description("Runs a pottery command (either package or test) every time a file is changed in the specified directory."))
            .add(CommandLine.Model.OptionSpec.builder("-i", "--polling-interval")
                    .description("Polling interval of the project folder, in milliseconds. Defaults to 250")
                    .paramLabel("POLLING_INTERVAL")
                    .defaultValue("250")
                    .type(int.class)
                    .build()
            )
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("0")
                    .paramLabel("DIRECTORY")
                    .description("Folder to watch. Defaults to the current directory.")
                    .defaultValue(".")
                    .type(String.class)
                    .build())
            .addSubcommand("package", PACKAGE_SPEC)
            .addSubcommand("test", TEST_SPEC)
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build());
    public static CommandLine.Model.CommandSpec CMD_SPEC = CommandLine.Model.CommandSpec.create()
            .name("./pottery.sh")
            .version(Toolchain.systemDefault().potteryVersion())
            .addSubcommand("watch", WATCH_SPEC)
            .addSubcommand("package", PACKAGE_SPEC)
            .addSubcommand("test", TEST_SPEC)
            .addSubcommand("idea", IDEA_SPEC)
            .addSubcommand("init", INIT_SPEC)
            .addSubcommand("clean", CLEAN_SPEC)
            .addSubcommand("purge", PURGE_SPEC)
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build())
            .addOption(CommandLine.Model.OptionSpec.builder("--version").versionHelp(true).build());
}
