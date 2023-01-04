/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.dependencies.maven;

import java.nio.file.Path;

public record DownloadedDependency(MavenDependency dependency, Path downloadPath) {
}
