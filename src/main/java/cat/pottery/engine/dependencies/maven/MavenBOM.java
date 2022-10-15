package cat.pottery.engine.dependencies.maven;

public record MavenBOM(
        String groupId,
        String artifactId,
        String version,
        Scope scope
) {
    public enum Scope {
        IMPORT
    }
}
