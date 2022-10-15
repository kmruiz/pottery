package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;

public final class DependencyDownloadWorker implements Runnable {
    private final Queue<MavenDependency> dependenciesToDownload;
    private final DownloadManager downloadManager;
    private final HttpClient httpClient;
    private final DocumentBuilder xmlBuilder;

    public DependencyDownloadWorker(DownloadManager downloadManager, Queue<MavenDependency> dependenciesToDownload) {
        this.dependenciesToDownload = dependenciesToDownload;
        this.downloadManager = downloadManager;
        this.httpClient = HttpClient.newBuilder().build();
        var factory = DocumentBuilderFactory.newInstance();
        try {
            this.xmlBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }
    }

    @Override
    public void run() {
        MavenDependency toDownload = null;
        do {
            toDownload = dependenciesToDownload.poll();
            if (toDownload == null) {
                break;
            }

            // first find transitive dependencies and offer them to download
            var transitiveDependencies = findTransitiveDependencies(toDownload);
            transitiveDependencies.forEach(downloadManager::trackDependency);

            // now download the jar
            if (shouldDownload(toDownload)) {
                downloadJar(toDownload);
            }
        } while (true);

        downloadManager.finished();
    }

    private List<MavenDependency> findTransitiveDependencies(MavenDependency mavenDependency) {
        try {
            var doc = xmlBuilder.parse(pomDownloadUrl(mavenDependency));
            var project = doc.getDocumentElement();

            Node dependenciesElement = null;

            for (var i = 0; i < project.getChildNodes().getLength(); i++) {
                var node = project.getChildNodes().item(i);
                if (node.getNodeName().equals("dependencies")) {
                    dependenciesElement = node;
                    break;
                }

            }

            if (dependenciesElement == null) {
                return Collections.emptyList();
            }

            List<MavenDependency> dependencies = new ArrayList<>(dependenciesElement.getChildNodes().getLength());
            for (var i = 0; i < dependenciesElement.getChildNodes().getLength(); i++) {
                var dependency = dependenciesElement.getChildNodes().item(i);
                if (!dependency.getNodeName().equals("dependency")) {
                    continue;
                }

                String groupId = null, artifactId = null, version = null;
                MavenDependency.Scope scope = MavenDependency.Scope.TEST;

                for (var j = 0; j < dependency.getChildNodes().getLength(); j++) {
                    var dependencyAttr = dependency.getChildNodes().item(j);
                    switch (dependencyAttr.getNodeName()) {
                        case "groupId" -> groupId = dependencyAttr.getTextContent();
                        case "artifactId" -> artifactId = dependencyAttr.getTextContent();
                        case "version" -> version = dependencyAttr.getTextContent();
                        case "scope" -> scope = MavenDependency.Scope.valueOf(dependencyAttr.getTextContent().trim().toUpperCase());
                    }
                }

                dependencies.add(new MavenDependency(groupId, artifactId, version, "jar", scope, "jar"));
            }

            return dependencies;
        } catch (Throwable e) {
            System.out.println("For " + mavenDependency + ": " + pomDownloadUrl(mavenDependency));
            throw new RuntimeException(e);
        }
    }

    private void downloadJar(MavenDependency dependency) {
        try {
            var whereToDownload = downloadManager.downloadPathOfDependency(dependency);
            var folder = whereToDownload.getParent();
            Files.createDirectories(folder);

            httpClient.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(
                                    URI.create(downloadUrl(dependency))
                            ).build(),
                    HttpResponse.BodyHandlers.ofFile(whereToDownload)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String pomDownloadUrl(MavenDependency dependency) {
        return downloadUrlWithQualifier(dependency, "pom");
    }

    private String downloadUrl(MavenDependency dependency) {
        return downloadUrlWithQualifier(dependency, dependency.qualifier());
    }

    private String downloadUrlWithQualifier(MavenDependency dependency, String qualifier) {
        return "https://repo1.maven.org/maven2/%s/%s/%s/%s".formatted(
                dependency.groupId().replaceAll("\\.", "/"),
                dependency.artifactId(),
                dependency.version(),
                "%s-%s.%s".formatted(dependency.artifactId(), dependency.version(), qualifier)
        );
    }

    private boolean shouldDownload(MavenDependency dependency) {
        if (dependency.isSnapshot()) {
            return true;
        }

        return !downloadManager.downloadPathOfDependency(dependency).toFile().exists();
    }
}
