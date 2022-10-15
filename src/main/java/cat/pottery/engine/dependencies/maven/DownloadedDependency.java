package cat.pottery.engine.dependencies.maven;

import java.nio.file.Path;

public record DownloadedDependency(MavenDependency dependency, Path downloadPath) {
}
