/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.output;

import cat.pottery.engine.dependencies.maven.DownloadedDependency;
import cat.pottery.ui.artifact.ArtifactDocument;

import java.nio.file.Path;
import java.util.List;

public interface ArtifactOutput {
    void generateArtifact(ArtifactDocument artifactDocument, Path compilerOutput, List<DownloadedDependency> classPath, Path outputPath);
}
