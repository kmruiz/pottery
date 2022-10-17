package cat.pottery.ui.cli.command;

public final class CommandResolver {
    public CliCommand byName(String name) {
        return switch (name.trim().toLowerCase()) {
            case "watch" -> new WatchCommand();
            case "package" -> new PackageCommand();
            default -> new PackageCommand();
        };
    }
}
