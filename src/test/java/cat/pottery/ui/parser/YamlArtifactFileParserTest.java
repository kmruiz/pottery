/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package cat.pottery.ui.parser;

import cat.pottery.ui.artifact.Dependency;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlArtifactFileParserTest {
    @Test
    @Tags({ @Tag("integration") })
    public void parses_a_valid_pottery_yaml_file() {
        var parser = new YamlArtifactFileParser();
        Path potteryYml = Path.of("src", "test", "resources", "integration-test", "yaml-parser", "valid-pottery.yaml");

        var result = parser.parse(potteryYml);

        assertTrue(result instanceof ArtifactFileParserResult.Success);

        var success = (ArtifactFileParserResult.Success) result;
        var document = success.document();

        assertEquals("5.9.1", document.parameters().get("junit.version"));
        assertEquals("4.7.0", document.parameters().get("picocli.version"));

        var artifact = document.artifact();

        assertEquals("cat.pottery.testing", artifact.group());
        assertEquals("valid.pottery", artifact.id());
        assertEquals("1.0.0", artifact.version());

        var platform = artifact.platform();
        assertEquals("17", platform.version());
        assertEquals("fatjar", platform.produces().get(0));
        assertEquals("container", platform.produces().get(1));

        var manifest = artifact.manifest();
        assertEquals("cat.pottery.ui.cli.Bootstrap", manifest.mainClass());

        var dependencies = artifact.dependencies();
        assertEquals(Dependency.Scope.PRODUCTION, dependencies.get(0).scope());
        assertEquals("info.picocli:picocli:${picocli.version}", dependencies.get(0).qualifiedName());
        assertEquals(Dependency.Scope.TEST, dependencies.get(1).scope());
        assertEquals("org.junit.jupiter:junit-jupiter-api:${junit.version}", dependencies.get(1).qualifiedName());
    }

    @Test
    @Tags({ @Tag("integration") })
    public void resolves_dependencies_from_the_yaml_file() {
        var parser = new YamlArtifactFileParser();
        Path potteryYml = Path.of("src", "test", "resources", "integration-test", "yaml-parser", "valid-pottery.yaml");

        var result = parser.parse(potteryYml);

        assertTrue(result instanceof ArtifactFileParserResult.Success);

        var success = (ArtifactFileParserResult.Success) result;
        var document = success.document();

        assertEquals("5.9.1", document.parameters().get("junit.version"));
        assertEquals("4.7.0", document.parameters().get("picocli.version"));

        var dependencies = document.resolvedDependencies();

        assertEquals(Dependency.Scope.PRODUCTION, dependencies.get(0).scope());
        assertEquals("info.picocli:picocli:4.7.0", dependencies.get(0).qualifiedName());
        assertEquals(Dependency.Scope.TEST, dependencies.get(1).scope());
        assertEquals("org.junit.jupiter:junit-jupiter-api:5.9.1", dependencies.get(1).qualifiedName());
    }
}
