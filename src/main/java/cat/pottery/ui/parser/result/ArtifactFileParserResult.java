/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.parser.result;

import cat.pottery.ui.artifact.ArtifactDocument;

import java.util.List;

public sealed interface ArtifactFileParserResult {
    record WarningMessage(String message) {}
    record ErrorMessage(String message) {}

    record Success(ArtifactDocument document, List<WarningMessage> warnings) implements ArtifactFileParserResult {}
    record Failure(List<ErrorMessage> errors) implements ArtifactFileParserResult {}
}
