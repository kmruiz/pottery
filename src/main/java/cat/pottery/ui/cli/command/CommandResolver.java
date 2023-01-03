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
            default -> new PackageCommand();
        };
    }

    private static CommandLine.Model.CommandSpec PACKAGE_SPEC = CommandLine.Model.CommandSpec.create();
    private static CommandLine.Model.CommandSpec IDEA_SPEC = CommandLine.Model.CommandSpec.create();
    private static CommandLine.Model.CommandSpec INIT_SPEC = CommandLine.Model.CommandSpec.create()
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("0")
                    .type(String.class)
                    .description("Directory where to initialise the project")
                    .build())
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("1")
                    .type(String.class)
                    .description("Group Id of the artifact")
                    .build())
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("2")
                    .type(String.class)
                    .description("Artifact Id of the artifact")
                    .build())
            .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                    .index("3")
                    .type(String.class)
                    .description("Version of the artifact")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("-j", "--jdk")
                    .type(String.class)
                    .description("Target JDK for the artifact.")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("-p", "--produces")
                    .type(String.class)
                    .description("Comma-separated list of artifact types that the project produce. Options: fatjar, library, container, docker, native.")
                    .build())
            ;

    private static CommandLine.Model.CommandSpec TEST_SPEC = CommandLine.Model.CommandSpec.create()
            .addOption(CommandLine.Model.OptionSpec.builder("-v", "--verbose")
                    .description("Show stack traces for failed tests.")
                    .build())
            .addOption(CommandLine.Model.OptionSpec.builder("-x", "--strict")
                    .description("Strict mode. Forces failure when there are skipped tests.")
                    .build());

    public static CommandLine.Model.CommandSpec CMD_SPEC = CommandLine.Model.CommandSpec.create()
            .version(Toolchain.systemDefault().potteryVersion())
            .addSubcommand("watch", CommandLine.Model.CommandSpec.create()
                    .add(CommandLine.Model.OptionSpec.builder("-i", "--polling-interval")
                            .description("Polling interval of the project folder, in milliseconds. Defaults to 250")
                            .paramLabel("POLLING_INTERVAL")
                            .defaultValue("250")
                            .type(int.class)
                            .build()
                    )
                    .addPositional(CommandLine.Model.PositionalParamSpec.builder()
                            .index("0")
                            .description("Folder to watch. Defaults to the current directory.")
                            .defaultValue(".")
                            .type(String.class)
                            .build())
                    .addSubcommand("package", PACKAGE_SPEC)
                    .addSubcommand("test", TEST_SPEC)
            ).addSubcommand("package", PACKAGE_SPEC)
            .addSubcommand("test", TEST_SPEC)
            .addSubcommand("idea", IDEA_SPEC)
            .addSubcommand("init", INIT_SPEC)
            .addOption(CommandLine.Model.OptionSpec.builder("--help").usageHelp(true).build())
            .addOption(CommandLine.Model.OptionSpec.builder("--version").versionHelp(true).build());
}
