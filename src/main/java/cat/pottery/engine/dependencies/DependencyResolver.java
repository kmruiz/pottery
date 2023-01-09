/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.telemetry.Log;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public final class DependencyResolver {
    private final DownloadManager downloadManager;
    private final Queue<MavenDependency> queue;
    private final int workerCount;
    private final Thread[] workers;
    private final PomContextRegistry pomContextRegistry;

    public DependencyResolver(DownloadManager downloadManager, Queue<MavenDependency> queue, int workerCount, PomContextRegistry pomContextRegistry) {
        this.downloadManager = downloadManager;
        this.queue = queue;
        this.workerCount = workerCount;
        this.pomContextRegistry = pomContextRegistry;
        this.workers = new Thread[this.workerCount];
    }

    public List<DownloadedDependency> downloadDependenciesOfArtifact(ArtifactDocument artifact) {
        artifact.resolvedDependencies().stream().map(dependency -> {
            var info = dependency.qualifiedName().split(":");
            return new MavenDependency(info[0], info[1], info[2], "jar", dependency.scope().toMavenScope(), "jar", Optional.empty());
        }).forEach(downloadManager::trackDependency);

        for (var i = 0; i < workerCount; i++) {
            this.workers[i] = new Thread(new DependencyDownloadWorker(downloadManager, queue, pomContextRegistry));
            this.workers[i].setName("dependency-download-worker-" + i);
            this.workers[i].setUncaughtExceptionHandler((t, ex) -> {
                Log.getInstance().error("Uncaught Exception in thread %s.", ex, t.getName());
            });
            this.workers[i].start();
        }

        return waitUntilFinishedDownloading();
    }

    private List<DownloadedDependency> waitUntilFinishedDownloading() {
        var downloadedDeps = downloadManager.downloadedDependencies();
        do {
            try {
                return downloadedDeps.get();
            } catch (Throwable e) {
                safeSleep();
            }
        } while (true);
    }

    private void safeSleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {

        }
    }
}
