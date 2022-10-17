package cat.pottery.ui.cli.command;

import cat.pottery.ui.artifact.ArtifactDocument;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

public final class WatchCommand implements CliCommand {

    @Override
    public void execute(String[] args) {
        CliCommand target = new PackageCommand();
        if (args.length == 3) {
            target = new CommandResolver().byName(args[2]);
        }

        Path dir = Path.of(".").toAbsolutePath();
        System.out.println("watching " + dir);

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
                    key = watcher.poll(250, TimeUnit.MILLISECONDS);
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
                }

                target.execute(args);

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
