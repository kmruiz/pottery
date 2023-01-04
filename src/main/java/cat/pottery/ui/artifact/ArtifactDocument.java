package cat.pottery.ui.artifact;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public record ArtifactDocument(
        Map<String, String> parameters,
        Artifact artifact
) {
    public List<Dependency> resolvedDependencies() {
        return artifact.dependencies().stream().map(dependency -> {
            var depQn = new AtomicReference<>(dependency.qualifiedName());

            parameters.forEach((key, value) -> depQn.getAndUpdate(qn -> qn.replace("${" + key + "}", value)));

            return new Dependency(dependency.scope(), depQn.get());
        }).toList();
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Artifact getArtifact() {
        return artifact;
    }
}
