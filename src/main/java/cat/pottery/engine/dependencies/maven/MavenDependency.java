package cat.pottery.engine.dependencies.maven;

import java.util.regex.Pattern;

public record MavenDependency(
        String groupId,
        String artifactId,
        String version,
        String type,
        Scope scope,
        String qualifier
) {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)");

    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }

    public boolean isCompatibleWith(MavenDependency mavenDependency) {
        if (version.equals(mavenDependency.version)) {
            return true;
        }

        var thisMatcher = VERSION_PATTERN.matcher(version);
        var otherMatcher = VERSION_PATTERN.matcher(mavenDependency.version);

        if (!thisMatcher.matches() || !otherMatcher.matches()) {
            return false;
        }

        var thisMajor = Integer.parseInt(thisMatcher.group("major"));
        var thisMinor = Integer.parseInt(thisMatcher.group("minor"));
        var thisPatch = Integer.parseInt(thisMatcher.group("patch"));

        var otherMajor = Integer.parseInt(otherMatcher.group("major"));
        var otherMinor = Integer.parseInt(otherMatcher.group("minor"));
        var otherPatch = Integer.parseInt(otherMatcher.group("patch"));

        if (thisMajor != otherMajor) {
            return false;
        }

        if (thisMinor != otherMinor) {
            return false;
        }

        return thisPatch >= otherPatch;
    }

    public MavenDependency max(MavenDependency dependency) {
        if (!isCompatibleWith(dependency)) {
            return null;
        }

        var thisMatcher = VERSION_PATTERN.matcher(version);
        var otherMatcher = VERSION_PATTERN.matcher(dependency.version);

        if (!thisMatcher.matches() || !otherMatcher.matches()) {
            return null;
        }

        var thisPatch = Integer.parseInt(thisMatcher.group("patch"));
        var otherPatch = Integer.parseInt(otherMatcher.group("patch"));

        if (thisPatch > otherPatch) {
            return this;
        }

        return dependency;
    }
    public enum Scope {
        COMPILE, RUNTIME, TEST
    }
}
