/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.parser;

import cat.pottery.ui.parser.result.ArtifactFileParserResult;

import java.nio.file.Path;

public interface ArtifactFileParser {
    ArtifactFileParserResult parse(Path path);
}
