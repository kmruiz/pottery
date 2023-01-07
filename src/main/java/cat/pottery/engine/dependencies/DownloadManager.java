/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.engine.integrations.IDEAImlGenerator;
import cat.pottery.telemetry.Log;

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
    private final boolean downloadTestDependencies;

    public DownloadManager(Queue<MavenDependency> dependenciesToDownload, Map<String, Set<MavenDependency>> foundVersionsPerArtifact, int expectFinishingCount, boolean downloadTestDependencies) {
        this.dependenciesToDownload = dependenciesToDownload;
        this.foundVersionsPerArtifact = foundVersionsPerArtifact;
        this.expectFinishingCount = expectFinishingCount;
        this.finishCount = new AtomicInteger(0);
        this.downloadTestDependencies = downloadTestDependencies;
    }

    public void trackDependency(MavenDependency dependency) {
        if (dependency.scope() == MavenDependency.Scope.TEST && !downloadTestDependencies) {
            return;
        }

        markDependencyToDownload(dependency);
    }

    public void trackTransitiveDependency(MavenDependency dependency) {
        if (dependency.scope() == MavenDependency.Scope.TEST) {
            return;
        }

        markDependencyToDownload(dependency);
    }

    private void markDependencyToDownload(MavenDependency dependency) {
        var qname = "%s:%s".formatted(dependency.groupId(), dependency.artifactId());
        var versionsOfDep = foundVersionsPerArtifact.getOrDefault(qname, new HashSet<>());

        if (versionsOfDep.stream().noneMatch(e -> e.version().equals(dependency.version()))) {
            if (!versionsOfDep.isEmpty()) {
                // warning!
                Log.getInstance().warn("Using multiple versions of %s:%s", dependency.groupId(), dependency.artifactId());
            }

            dependenciesToDownload.offer(dependency);
            versionsOfDep.add(dependency);
            foundVersionsPerArtifact.put(qname, versionsOfDep);
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

            var allDependencies = foundVersionsPerArtifact.values()
                    .stream()
                    .map(deps -> deps.stream().reduce(MavenDependency::max))
                    .map(Optional::get)
                    .map(dep -> new DownloadedDependency(dep, downloadPathOfDependency(dep)))
                    .toList();

            IDEAImlGenerator.getInstance().generateImlFileIfNecessary(allDependencies);
            return allDependencies;
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
