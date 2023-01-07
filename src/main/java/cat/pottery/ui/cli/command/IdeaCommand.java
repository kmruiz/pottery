/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.cli.command;

import cat.pottery.engine.integrations.IDEAImlGenerator;
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

    @Override
    public void execute(CommandLine.ParseResult parseResult) {
        Timing.getInstance().start(TIMING_ID);
        try {
            IDEAImlGenerator.getInstance().activateIntegration();
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
