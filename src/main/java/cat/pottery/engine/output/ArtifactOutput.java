package cat.pottery.engine.output;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.nio.file.Path;
import java.util.List;

public interface ArtifactOutput {
    void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath);
}
