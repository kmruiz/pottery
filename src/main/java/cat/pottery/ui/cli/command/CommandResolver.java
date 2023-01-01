package cat.pottery.ui.cli.command;

import cat.pottery.engine.toolchain.Toolchain;
import picocli.CommandLine;

public final class CommandResolver {
    public CliCommand byName(String name) {
        return switch (name.trim().toLowerCase()) {
            case "watch" -> new WatchCommand();
            case "package" -> new PackageCommand();
            case "test" -> new TestCommand();
            default -> new PackageCommand();
        };
    }

    private static CommandLine.Model.CommandSpec PACKAGE_SPEC = CommandLine.Model.CommandSpec.create()
            .helpCommand(true);

    private static CommandLine.Model.CommandSpec TEST_SPEC = CommandLine.Model.CommandSpec.create()
            .helpCommand(true);

    public static CommandLine.Model.CommandSpec CMD_SPEC = CommandLine.Model.CommandSpec.create()
            .helpCommand(true)
            .version(Toolchain.systemDefault().potteryVersion())
            .addSubcommand("watch", CommandLine.Model.CommandSpec.create()
                    .helpCommand(true)
                    .add(CommandLine.Model.OptionSpec.builder("-i", "--polling-interval")
                            .help(true)
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
            .addSubcommand("test", TEST_SPEC);
}
