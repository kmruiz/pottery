package cat.pottery.ui.parser;

import cat.pottery.ui.parser.result.ArtifactFileParserResult;

import java.nio.file.Path;

public interface ArtifactFileParser {
    ArtifactFileParserResult parse(Path path);
}
