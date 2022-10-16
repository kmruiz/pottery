package cat.pottery.ui.artifact;

import cat.pottery.ui.artifact.Dependency;
import cat.pottery.ui.artifact.Platform;

import java.util.List;

public record Artifact(
        String group,
        String id,
        String version,
        Platform platform,
        List<Dependency> dependencies,
        Manifest manifest
) {
}
