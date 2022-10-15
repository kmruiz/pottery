package cat.pottery.ui;

import cat.pottery.ui.artifact.Artifact;

import java.util.Map;

public record ArtifactDocument(
        Map<String, String> parameters,
        Artifact artifact
) {
}
