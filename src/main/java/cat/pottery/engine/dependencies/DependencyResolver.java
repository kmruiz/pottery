package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.ui.ArtifactDocument;
import cat.pottery.ui.artifact.Artifact;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;

import javax.xml.parsers.ParserConfigurationException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;

public final class DependencyResolver {
    private final DownloadManager downloadManager;
    private final Queue<MavenDependency> queue;
    private final int workerCount;
    private final Thread[] workers;

    public DependencyResolver(DownloadManager downloadManager, Queue<MavenDependency> queue, int workerCount) {
        this.downloadManager = downloadManager;
        this.queue = queue;
        this.workerCount = workerCount;
        this.workers = new Thread[this.workerCount];
    }

    public List<DownloadedDependency> downloadDependenciesOfArtifact(ArtifactDocument artifact) {
        artifact.resolvedDependencies().stream().map(dependency -> {
            var info = dependency.qualifiedName().split(":");
            return new MavenDependency(info[0], info[1], info[2], "jar", MavenDependency.Scope.valueOf(dependency.scope().name()), "jar");
        }).forEach(downloadManager::trackDependency);

        for (var i = 0; i < workerCount; i++) {
            this.workers[i] = new Thread(new DependencyDownloadWorker(downloadManager, queue));
            this.workers[i].start();
        }

        return waitUntilFinishedDownloading();
    }

    private List<DownloadedDependency> waitUntilFinishedDownloading() {
        try {
            return downloadManager.downloadedDependencies().get();
        } catch (InterruptedException | ExecutionException e) {
            return waitUntilFinishedDownloading();
        }
    }


    public static void main(String[] args) {
        var queue = new ArrayBlockingQueue<MavenDependency>(128);
        var manager = new DownloadManager(queue, new HashMap<>(), 4);

        var dependencyResolver = new DependencyResolver(manager, queue, 4);
        var artifactDoc = (ArtifactFileParserResult.Success) new YamlArtifactFileParser().parse(Path.of("/home/kevin/test.yaml"));

        var deps = dependencyResolver.downloadDependenciesOfArtifact(artifactDoc.document());
        deps.forEach(System.out::println);
    }
}
