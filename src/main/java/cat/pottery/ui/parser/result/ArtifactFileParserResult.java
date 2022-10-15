package cat.pottery.ui.parser.result;

import cat.pottery.ui.ArtifactDocument;
import cat.pottery.ui.artifact.Artifact;

import java.util.List;

public sealed interface ArtifactFileParserResult {
    record WarningMessage(String message) {}
    record ErrorMessage(String message) {}

    record Success(ArtifactDocument document, List<WarningMessage> warnings) implements ArtifactFileParserResult {}
    record Failure(List<ErrorMessage> errors) implements ArtifactFileParserResult {}
}
