package cat.pottery.ui.artifact;

import java.util.List;

public record Artifact(
        String group,
        String id,
        String version,
        Platform platform,
        List<Dependency> dependencies,
        Manifest manifest
) {
    public String getGroup() {
        return group;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public Platform getPlatform() {
        return platform;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public Manifest getManifest() {
        return manifest;
    }
}
