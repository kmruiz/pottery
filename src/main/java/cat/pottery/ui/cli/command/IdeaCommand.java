package cat.pottery.ui.cli.command;

import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import picocli.CommandLine;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

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
