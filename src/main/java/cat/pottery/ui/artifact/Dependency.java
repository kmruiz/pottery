package cat.pottery.ui.artifact;

import cat.pottery.engine.dependencies.maven.MavenDependency;

public record Dependency(
        Scope scope,
        String qualifiedName
) {
    public interface CanScopeToMaven {
        MavenDependency.Scope toMavenScope();
    }
    public enum Scope implements CanScopeToMaven {
        PRODUCTION {
            @Override
            public MavenDependency.Scope toMavenScope() {
                return MavenDependency.Scope.COMPILE;
            }
        }, TEST {
            @Override
            public MavenDependency.Scope toMavenScope() {
                return MavenDependency.Scope.TEST;
            }
        };
    }
}
