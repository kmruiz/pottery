package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

import static org.w3c.dom.Node.ELEMENT_NODE;

public final class DependencyDownloadWorker implements Runnable {
    private final Queue<MavenDependency> dependenciesToDownload;
    private final DownloadManager downloadManager;
    private final HttpClient httpClient;
    private final DocumentBuilder xmlBuilder;
    private final PomContextRegistry pomContextRegistry;

    public DependencyDownloadWorker(DownloadManager downloadManager, Queue<MavenDependency> dependenciesToDownload, PomContextRegistry pomContextRegistry) {
        this.dependenciesToDownload = dependenciesToDownload;
        this.downloadManager = downloadManager;
        this.pomContextRegistry = pomContextRegistry;
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
            Node propertiesElement = null;

            boolean hasParent = false;
            String groupId = null, artifactId = null, version = "<default>";
            String parentGroupId = null, parentArtifactId = null, parentVersion = "<default>";

            for (var i = 0; i < project.getChildNodes().getLength(); i++) {
                var node = project.getChildNodes().item(i);
                if (node.getNodeName().equals("dependencies")) {
                    dependenciesElement = node;
                } else if (node.getNodeName().equals("properties")) {
                    propertiesElement = node;
                } else if (node.getNodeName().equals("groupId")) {
                    groupId = node.getTextContent();
                } else if (node.getNodeName().equals("artifactId")) {
                    artifactId = node.getTextContent();
                } else if (node.getNodeName().equals("version")) {
                    version = node.getTextContent();
                } else if (node.getNodeName().equals("parent")) {
                    hasParent = true;
                    for (var j = 0; j < node.getChildNodes().getLength(); j++) {
                        var parentNode = node.getChildNodes().item(j);
                        if (parentNode.getNodeName().equals("groupId")) {
                            parentGroupId = parentNode.getTextContent();
                        } else if (parentNode.getNodeName().equals("artifactId")) {
                            parentArtifactId = parentNode.getTextContent();
                        } else if (parentNode.getNodeName().equals("version")) {
                            parentVersion = parentNode.getTextContent();
                        }
                    }
                }
            }

            var context = hasParent ? pomContextRegistry.registerFromParent(parentGroupId, parentArtifactId, parentVersion, groupId, artifactId, version)
                    : pomContextRegistry.register(groupId, artifactId, version);

            if (propertiesElement != null) {
                for (var i = 0; i < propertiesElement.getChildNodes().getLength(); i++) {
                    var property = propertiesElement.getChildNodes().item(i);
                    if (property.getNodeType() == ELEMENT_NODE) {
                        pomContextRegistry.addParameter(context, property.getNodeName(), property.getTextContent());
                    }
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

                String depGroupId = null, depArtifactId = null, depVersion = null;
                MavenDependency.Scope scope = MavenDependency.Scope.RUNTIME;
                Optional<String> classifier = Optional.empty();

                for (var j = 0; j < dependency.getChildNodes().getLength(); j++) {
                    var dependencyAttr = dependency.getChildNodes().item(j);
                    switch (dependencyAttr.getNodeName()) {
                        case "groupId" -> depGroupId = dependencyAttr.getTextContent();
                        case "artifactId" -> depArtifactId = dependencyAttr.getTextContent();
                        case "version" -> depVersion = dependencyAttr.getTextContent();
                        case "scope" -> scope = MavenDependency.Scope.valueOf(dependencyAttr.getTextContent().trim().toUpperCase());
                        case "classifier" -> classifier = Optional.of(dependencyAttr.getTextContent());
                    }
                }

                dependencies.add(
                        new MavenDependency(
                                pomContextRegistry.resolveExpression(context, depGroupId),
                                pomContextRegistry.resolveExpression(context, depArtifactId),
                                pomContextRegistry.resolveExpression(context, depVersion),
                                "jar",
                                scope,
                                "jar",
                                classifier
                        )
                );
            }

            return dependencies;
        } catch (FileNotFoundException e) {
            return Collections.emptyList();
        } catch (Throwable e) {
            Log.getInstance().error("Could not download dependency %s:%s:%s POM file from %s.", e, mavenDependency.groupId(), mavenDependency.artifactId(), mavenDependency.version(), pomDownloadUrl(mavenDependency));
            return Collections.emptyList();
        }
    }

    private void downloadJar(MavenDependency dependency) {
        try {

            var whereToDownload = downloadManager.downloadPathOfDependency(dependency);
            var folder = whereToDownload.getParent();
            Files.createDirectories(folder);

            Timing.getInstance().start(dependency.toString());
            httpClient.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(
                                    URI.create(downloadUrl(dependency))
                            ).build(),
                    HttpResponse.BodyHandlers.ofFile(whereToDownload)
            );
            Timing.getInstance().end(dependency.toString());

            var downloadDuration = Timing.getInstance().durationOf(dependency.toString());

            Log.getInstance().info("Downloading %s:%s:%s:%s for %s in %dms.", dependency.groupId(), dependency.artifactId(), dependency.version(), dependency.qualifier(), dependency.scope().reason(), downloadDuration.toMillis());

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
                "%s-%s%s.%s".formatted(dependency.artifactId(), dependency.version(), dependency.classifier().map(e -> "-" + e).orElse(""), qualifier)
        );
    }

    private boolean shouldDownload(MavenDependency dependency) {
        if (dependency.isSnapshot()) {
            return true;
        }

        return !downloadManager.downloadPathOfDependency(dependency).toFile().exists();
    }
}
