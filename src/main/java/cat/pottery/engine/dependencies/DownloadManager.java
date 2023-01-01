package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.dependencies.maven.MavenDependency;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class DownloadManager {
    private final Queue<MavenDependency> dependenciesToDownload;
    private final Map<String, Set<MavenDependency>> foundVersionsPerArtifact;
    private final int expectFinishingCount;
    private final AtomicInteger finishCount;

    public DownloadManager(Queue<MavenDependency> dependenciesToDownload, Map<String, Set<MavenDependency>> foundVersionsPerArtifact, int expectFinishingCount) {
        this.dependenciesToDownload = dependenciesToDownload;
        this.foundVersionsPerArtifact = foundVersionsPerArtifact;
        this.expectFinishingCount = expectFinishingCount;
        this.finishCount = new AtomicInteger(0);
    }

    public void trackDependency(MavenDependency dependency) {
        if (dependency.scope() == MavenDependency.Scope.TEST) {
            return;
        }

        var qname = "%s:%s".formatted(dependency.groupId(), dependency.artifactId());
        var versionsOfDep = foundVersionsPerArtifact.getOrDefault(qname, new HashSet<>());

        if (versionsOfDep.stream().noneMatch(e -> e.version().equals(dependency.version()))) {
            if (!versionsOfDep.isEmpty()) {
                // warning!
                System.out.println("Downloading other version of " + dependency);
            }

            dependenciesToDownload.offer(dependency);
            versionsOfDep.add(dependency);
            foundVersionsPerArtifact.put(qname, versionsOfDep);
        } else {
            // System.out.println("Already downloaded " + dependency);
        }
    }

    public void finished() {
        this.finishCount.incrementAndGet();
    }

    public Future<List<DownloadedDependency>> downloadedDependencies() {
        return CompletableFuture.supplyAsync(() -> {
            while (this.finishCount.get() < this.expectFinishingCount) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
            }

            return foundVersionsPerArtifact.values()
                    .stream()
                    .map(deps -> deps.stream().reduce(MavenDependency::max))
                    .map(Optional::get)
                    .map(dep -> new DownloadedDependency(dep, downloadPathOfDependency(dep)))
                    .toList();
        });
    }

    public Path downloadPathOfDependency(MavenDependency dependency) {
        return Path.of(
                ".pottery",
                "m2",
                dependency.groupId(),
                dependency.artifactId(),
                dependency.version(),
                "%s-%s%s.%s".formatted(dependency.artifactId(), dependency.version(), dependency.classifier().map(e -> "-" + e).orElse(""), dependency.qualifier())
        );
    }
}
