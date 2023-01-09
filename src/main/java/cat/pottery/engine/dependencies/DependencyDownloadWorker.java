/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies;

import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

import static org.w3c.dom.Node.ELEMENT_NODE;

public final class DependencyDownloadWorker implements Runnable {
    private record TransitiveDependencies(
            Optional<MavenDependency> parentDependency,
            List<MavenDependency> mavenDependencies
    ) {}

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
        do {
            try {
                var toDownload = dependenciesToDownload.poll();
                if (toDownload == null || toDownload.version() == null) {
                    safeSleep();
                    break;
                }

                // first find transitive dependencies and offer them to download
                var transitiveDependencies = findTransitiveDependencies(toDownload);

                if (transitiveDependencies.parentDependency.isPresent()) {
                    var parentDependency = transitiveDependencies.parentDependency.get();
                    if (!pomContextRegistry.hasContext(pomContextRegistry.contextIdFor(parentDependency))) {
                        downloadManager.trackTransitiveDependency(parentDependency);
                        dependenciesToDownload.offer(toDownload);
                        continue;
                    }
                }

                transitiveDependencies.mavenDependencies.forEach(downloadManager::trackTransitiveDependency);

                // now download the jar
                if (shouldDownload(toDownload)) {
                    downloadJar(toDownload);
                }
            } catch (Throwable e) {
                Log.getInstance().error("Could not download dependency", e);
                break;
            }
        } while (true);

        downloadManager.finished();
    }

    private TransitiveDependencies findTransitiveDependencies(MavenDependency mavenDependency) {
        try {
            var doc = xmlBuilder.parse(pomDownloadUrl(mavenDependency));
            var project = doc.getDocumentElement();

            Node dependenciesElement = null;
            Node propertiesElement = null;
            Node dependencyManagementDependencies = null;

            MavenDependency parentDependency = null;

            boolean hasParent = false;
            String groupId = null, artifactId = null, version = null;
            String parentGroupId = null, parentArtifactId = null, parentVersion = null;

            var node = project.getFirstChild();
            while (node != null) {
                switch (node.getNodeName()) {
                    case "dependencies" -> dependenciesElement = node;
                    case "properties" -> propertiesElement = node;
                    case "groupId" -> groupId = node.getTextContent();
                    case "artifactId" -> artifactId = node.getTextContent();
                    case "version" -> version = node.getTextContent();
                    case "parent" -> {
                        hasParent = true;
                        var parentNode = ((Element) node.getChildNodes()).getFirstChild();
                        while (parentNode != null) {
                            switch (parentNode.getNodeName()) {
                                case "groupId" -> parentGroupId = parentNode.getTextContent();
                                case "artifactId" -> parentArtifactId = parentNode.getTextContent();
                                case "version" -> parentVersion = parentNode.getTextContent();
                            }

                            parentNode = parentNode.getNextSibling();
                        }
                    }
                    case "dependencyManagement" -> {
                        var dmNode = node.getFirstChild();
                        while (dmNode != null) {
                            if (dmNode.getNodeName().equals("dependencies")) {
                                dependencyManagementDependencies = dmNode;
                                break;
                            }

                            dmNode = dmNode.getNextSibling();
                        }
                    }
                }

                node = node.getNextSibling();
            }

            groupId = Objects.requireNonNullElse(groupId, parentGroupId);
            version = Objects.requireNonNullElse(version, parentVersion);

            if (hasParent && !pomContextRegistry.hasContext(pomContextRegistry.contextIdFor(parentGroupId, parentArtifactId, parentVersion))) {
                return new TransitiveDependencies(Optional.of(new MavenDependency(parentGroupId, parentArtifactId, parentVersion, "pom", MavenDependency.Scope.COMPILE, "", Optional.empty())), Collections.emptyList());
            }

            String context;
            if (hasParent) {
                parentDependency = new MavenDependency(parentGroupId, parentArtifactId, parentVersion, "pom", MavenDependency.Scope.COMPILE, "", Optional.empty());
                context = pomContextRegistry.registerFromParent(parentGroupId, parentArtifactId, parentVersion, groupId, artifactId, version);
            } else {
                context = pomContextRegistry.register(groupId, artifactId, version);
            }

            if (propertiesElement != null) {
                var property = propertiesElement.getFirstChild();
                while (property != null) {
                    if (property.getNodeType() == ELEMENT_NODE) {
                        pomContextRegistry.addParameter(context, property.getNodeName(), property.getTextContent());
                    }

                    property = property.getNextSibling();
                }
            }

            if (dependencyManagementDependencies != null) {
                doForEachDependency(dependencyManagementDependencies, context, dependency -> pomContextRegistry.addVersionSuggestion(
                        context,
                        dependency.qualifiedName(),
                        dependency.version()
                ));
            }

            if (dependenciesElement == null) {
                return new TransitiveDependencies(Optional.ofNullable(parentDependency), Collections.emptyList());
            }

            List<MavenDependency> dependencies = new LinkedList<>();
            doForEachDependency(dependenciesElement, context, dependencies::add);

            return new TransitiveDependencies(Optional.ofNullable(parentDependency), dependencies);
        } catch (FileNotFoundException e) {
            return new TransitiveDependencies(Optional.empty(), Collections.emptyList());
        } catch (Throwable e) {
            Log.getInstance().error("Could not download dependency %s:%s:%s POM file from %s.", e, mavenDependency.groupId(), mavenDependency.artifactId(), mavenDependency.version(), pomDownloadUrl(mavenDependency));
            return new TransitiveDependencies(Optional.empty(), Collections.emptyList());
        }
    }

    private void doForEachDependency(Node dependenciesElement, String context, Consumer<MavenDependency> consumer) {
        var dependency = dependenciesElement.getFirstChild();
        while (dependency != null) {
            if (!dependency.getNodeName().equals("dependency")) {
                dependency = dependency.getNextSibling();
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

            consumer.accept(
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

            dependency = dependency.getNextSibling();
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
            var downloadDuration = Timing.getInstance().end(dependency.toString());

            Log.getInstance().info("Downloaded %s:%s:%s:%s for %s in %s.", dependency.groupId(), dependency.artifactId(), dependency.version(), dependency.qualifier(), dependency.scope().reason(), downloadDuration);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String downloadUrl(MavenDependency dependency) {
        return "https://repo1.maven.org/maven2/%s/%s/%s/%s".formatted(
                dependency.groupId().replaceAll("\\.", "/"),
                dependency.artifactId(),
                dependency.version(),
                "%s-%s%s.%s".formatted(dependency.artifactId(), dependency.version(), dependency.classifier().map(e -> "-" + e).orElse(""), dependency.qualifier())
        );
    }

    private String pomDownloadUrl(MavenDependency dependency) {
        var pathOfPom = downloadManager.downloadPathOfPOM(dependency);
        if (pathOfPom.toFile().exists()) {
            try {
                return pathOfPom.toUri().toURL().toString();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            String externalUrlOfPom = "https://repo1.maven.org/maven2/%s/%s/%s/%s".formatted(
                    dependency.groupId().replaceAll("\\.", "/"),
                    dependency.artifactId(),
                    dependency.version(),
                    "%s-%s%s.pom".formatted(dependency.artifactId(), dependency.version(), dependency.classifier().map(e -> "-" + e).orElse(""))
            );

            try {
                var folder = pathOfPom.getParent();
                Files.createDirectories(folder);

                httpClient.send(
                        HttpRequest.newBuilder()
                                .GET()
                                .uri(
                                        URI.create(externalUrlOfPom)
                                ).build(),
                        HttpResponse.BodyHandlers.ofFile(pathOfPom)
                );

                return pathOfPom.toUri().toURL().toString();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean shouldDownload(MavenDependency dependency) {
        if (dependency.type().equals("pom")) {
            return false;
        }

        if (dependency.isSnapshot()) {
            return true;
        }

        return !downloadManager.downloadPathOfDependency(dependency).toFile().exists();
    }

    private void safeSleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
