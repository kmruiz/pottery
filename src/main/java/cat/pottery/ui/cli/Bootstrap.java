package cat.pottery.ui.cli;

import cat.pottery.ui.cli.command.CliCommand;
import cat.pottery.ui.cli.command.CommandResolver;
import com.github.tomaslanger.chalk.Chalk;

public final class Bootstrap {
    public static void main(String[] args) {
        System.out.println(Chalk.on("COLORED").bgBlack().yellow().underline());
        if (args.length < 1) {
            return;
        }


        CliCommand cmd = new CommandResolver().byName(args[0]);
        cmd.execute(args);

    }
}
