/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.cli.command;

import cat.pottery.telemetry.Log;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

public final class WatchCommand implements CliCommand {
    public static boolean IS_WATCHING = false;


    public void execute(CommandLine.ParseResult parseResult) {
        CliCommand target = new PackageCommand();
        CommandLine.ParseResult innerParseResult = parseResult;
        var cmdName = "package";
        IS_WATCHING = true;

        if (parseResult.hasSubcommand()) {
            var watchSubcmd = parseResult.subcommand();
            cmdName = watchSubcmd.commandSpec().name();
            target = new CommandResolver().byName(cmdName);
            innerParseResult = watchSubcmd;
        }

        long pollingTime = parseResult.matchedOptionValue('i', 250);
        Path dir = Path.of(parseResult.matchedPositionalValue(0, ".")).toAbsolutePath();
        Log.getInstance().info("Watching directory %s every %d ms.", dir.toString(), pollingTime);

        try (var watcher = FileSystems.getDefault().newWatchService()) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                }
            });


            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            for (;;) {
                WatchKey key;
                try {
                    key = watcher.poll(pollingTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException x) {
                    return; //
                }

                if (key == null) {
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    Log.getInstance().info("Detected change on %s", filename.toString());
                }

                Log.getInstance().info("Running subcommand %s", cmdName);
                target.execute(innerParseResult);

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException e) {
            Log.getInstance().fatal("Exception watching directory: %s", e, e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
