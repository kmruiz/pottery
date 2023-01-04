/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.artifact;

import java.util.List;

public record Platform(
        String version,
        List<String> produces
) {
    public String getVersion() {
        return version;
    }

    public List<String> getProduces() {
        return produces;
    }
}
