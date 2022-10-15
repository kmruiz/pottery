package cat.pottery.ui.artifact;

public record Dependency(
        Scope scope,
        String qualifiedName
) {
    public enum Scope {
        PRODUCTION, TEST
    }
}
