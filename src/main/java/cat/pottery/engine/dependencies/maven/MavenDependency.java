/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies.maven;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record MavenDependency(
        String groupId,
        String artifactId,
        String version,
        String type,
        Scope scope,
        String qualifier,
        Optional<String> classifier
) implements Comparable<MavenDependency> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)(\\.(?<patch>\\d+)).*?");
    private static final Pattern IS_RANGE = Pattern.compile("[(\\[].+,.+[)\\]]");

    public String qualifiedName() {
        return "%s:%s".formatted(groupId, artifactId);
    }

    public MavenDependency withVersionIfUnspecified(String version) {
        if (version != null && (this.version == null || this.version.isBlank())) {
            return new MavenDependency(groupId, artifactId, version, type, scope, qualifier, classifier);
        }

        return this;
    }

    public String decidedVersion() {
        if (IS_RANGE.asMatchPredicate().test(version)) {
            return version.split(",")[1].trim().replace("]", "").replace(")", "");
        }

        return version;
    }
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
        var thisPatch = Integer.parseInt(Objects.requireNonNullElse(thisMatcher.group("patch"), "0"));

        var otherMajor = Integer.parseInt(otherMatcher.group("major"));
        var otherMinor = Integer.parseInt(otherMatcher.group("minor"));
        var otherPatch = Integer.parseInt(Objects.requireNonNullElse(otherMatcher.group("patch"), "0"));

        if (thisMajor != otherMajor) {
            return false;
        }

        if (thisMinor != otherMinor) {
            return false;
        }

        return true;
    }

    public MavenDependency max(MavenDependency dependency) {
        if (!isCompatibleWith(dependency)) {
            return this;
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

    @Override
    public int compareTo(MavenDependency o) {
        if (max(o) == this) {
            return -1;
        }

        return 1;
    }

    public boolean isNotVersioned() {
        return version == null || version.isBlank();
    }

    public enum Scope {
        COMPILE("packaging"), RUNTIME("packaging"), PROVIDED("provided"), IMPORT("provided"), TEST("testing");

        private final String reason;

        Scope(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
