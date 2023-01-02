package cat.pottery.ui.cli.command;

import cat.pottery.engine.compiler.IncrementalCompiler;
import cat.pottery.engine.dependencies.DependencyResolver;
import cat.pottery.engine.dependencies.DownloadManager;
import cat.pottery.engine.dependencies.PomContextRegistry;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import picocli.CommandLine;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;

public class IdeaCommand implements CliCommand {
    private static final String TIMING_ID = "idea-command";
    private static final String IDEA_WORKSPACE_XML = ".idea/workspace.xml";

    @Override
    public void execute(CommandLine.ParseResult parseResult) {
        Timing.getInstance().start(TIMING_ID);
        try {
            Files.createDirectories(Path.of(".idea"));

            if (!Files.exists(Path.of(IDEA_WORKSPACE_XML))) {
                Files.writeString(Path.of(IDEA_WORKSPACE_XML), getWorkspaceXmlWhenEmpty(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                return;
            }

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(IDEA_WORKSPACE_XML);

            Element project = (Element) document.getElementsByTagName("project").item(0);
            var allComponents = project.getElementsByTagName("component");

            boolean hasMavenImportPreferences = false;
            for (var i = 0; i < allComponents.getLength(); i++) {
                var component = (Element) allComponents.item(i);
                if ("MavenImportPreferences".equals(component.getAttribute("name"))) {
                    hasMavenImportPreferences = true;
                    var generalSettings = getOptionByName(component, "generalSettings");
                    var mavenSettings = generalSettings.getElementsByTagName("MavenGeneralSettings");
                    if (mavenSettings.getLength() == 1) {
                        var localRepoOption = getOptionByName((Element) mavenSettings.item(0), "localRepository");
                        if (localRepoOption == null) {
                            Element option = document.createElement("option");
                            option.setAttribute("name", "localRepository");
                            option.setAttribute("value", "$PROJECT_DIR$/.pottery/m2");
                            mavenSettings.item(0).appendChild(option);
                        } else {
                            localRepoOption.setAttribute("value", "$PROJECT_DIR$/.pottery/m2");
                        }

                    }
                }
            }

            if (!hasMavenImportPreferences) {
                Element component = document.createElement("component");
                component.setAttribute("name", "MavenImportPreferences");

                Element wrapperOption = document.createElement("option");
                wrapperOption.setAttribute("name", "generalSettings");

                Element mavenSettingNode = document.createElement("MavenGeneralSettings");
                Element option = document.createElement("option");
                option.setAttribute("name", "localRepository");
                option.setAttribute("value", "$PROJECT_DIR$/.pottery/m2");
                mavenSettingNode.appendChild(option);
                component.appendChild(mavenSettingNode);

                wrapperOption.appendChild(mavenSettingNode);
                component.appendChild(wrapperOption);
                project.appendChild(component);
            }

            DOMSource source = new DOMSource(document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(IDEA_WORKSPACE_XML);
            transformer.transform(source, result);
        } catch (Throwable ex) {
            Log.getInstance().error("Could not generate IntelliJ IDEA files.", ex);
        } finally {
            var duration = Timing.getInstance().end(TIMING_ID);
            Log.getInstance().info("Task ran in %s.", duration);
        }
    }

    private String getWorkspaceXmlWhenEmpty() {
        return new Scanner(IdeaCommand.class.getResourceAsStream("/workspace.xml"), "UTF-8").useDelimiter("\\A").next();
    }

    private Element getOptionByName(Element parent, String optionName) {
        var allOptions = parent.getElementsByTagName("option");
        for (var i = 0; i < allOptions.getLength(); i++) {
            var opt = (Element) allOptions.item(i);
            if (optionName.equals(opt.getAttribute("name"))) {
                return opt;
            }
        }

        return null;
    }
}
