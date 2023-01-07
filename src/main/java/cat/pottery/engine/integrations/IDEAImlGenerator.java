/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package cat.pottery.engine.integrations;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.telemetry.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class IDEAImlGenerator {
    private static final String TEMPLATE_FILE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <module type="JAVA_MODULE" version="4">
              <component name="NewModuleRootManager">
                <output url="file://$MODULE_DIR$/target/classes" />
                <output-test url="file://$MODULE_DIR$/target/test-classes" />
                <content url="file://$MODULE_DIR$">
                  <sourceFolder url="file://$MODULE_DIR$/src/main/java" isTestSource="false" />
                  <sourceFolder url="file://$MODULE_DIR$/src/main/resources" type="java-resource" />
                  <sourceFolder url="file://$MODULE_DIR$/src/test/java" isTestSource="true" />
                  <sourceFolder url="file://$MODULE_DIR$/src/test/resources" type="java-test-resource" />
                  <excludeFolder url="file://$MODULE_DIR$/target" />
                </content>
                <orderEntry type="inheritedJdk" />
                <orderEntry type="sourceFolder" forTests="false" />
                {{libraries}}
              </component>
            </module>
            """;

    private static final String ORDER_ENTRY_TEMPLATE = """
                <orderEntry type="module-library">
                  <library>
                    <CLASSES>
                      <root url="{{url}}" />
                    </CLASSES>
                    <JAVADOC />
                    <SOURCES />
                  </library>
                </orderEntry>
            """;

    public static IDEAImlGenerator getInstance() {
        return new IDEAImlGenerator();
    }

    public void activateIntegration() {
        try {
            Files.createDirectories(Path.of(".pottery"));
            Files.writeString(Path.of(".pottery", "idea-integration"), "");
        } catch (IOException e) {
            Log.getInstance().error("Could not activate IDEA integration.", e);
            throw new RuntimeException(e);
        }
    }
    public void generateImlFileIfNecessary(List<DownloadedDependency> dependencyList) {
        if (!Files.exists(Path.of(".pottery", "idea-integration"))) {
            return;
        }

        var allDependenciesFormatted = dependencyList.stream().map(this::generateDependencyXml).collect(Collectors.joining("\n"));
        var ideaXml = TEMPLATE_FILE.replace("{{libraries}}", allDependenciesFormatted);

        var folderName = Path.of(".").toAbsolutePath().getParent().getFileName();
        var imlFile = Path.of(".", ".idea", folderName + ".iml");
        try {
            Files.createDirectories(imlFile.getParent());
            Files.writeString(imlFile, ideaXml);
        } catch (IOException e) {
            Log.getInstance().error("Could not generate IDEA files for the project.", e);
            throw new RuntimeException(e);
        }
    }

    private String generateDependencyXml(DownloadedDependency dependency) {
        return ORDER_ENTRY_TEMPLATE.replace("{{url}}", pathForDependency(dependency));
    }
    private String pathForDependency(DownloadedDependency dependency) {
        var jarPath = dependency.downloadPath();
        var relativePathToModule = Path.of(".").relativize(jarPath);

        return "jar://$MODULE_DIR$/%s!/".formatted(relativePathToModule.toString());

    }
}
