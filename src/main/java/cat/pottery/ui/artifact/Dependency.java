/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.ui.artifact;

import cat.pottery.engine.dependencies.maven.MavenDependency;

public record Dependency(
        Scope scope,
        String qualifiedName
) {
    public interface CanScopeToMaven {
        MavenDependency.Scope toMavenScope();
    }
    public enum Scope implements CanScopeToMaven {
        PRODUCTION {
            @Override
            public MavenDependency.Scope toMavenScope() {
                return MavenDependency.Scope.COMPILE;
            }
        }, TEST {
            @Override
            public MavenDependency.Scope toMavenScope() {
                return MavenDependency.Scope.TEST;
            }
        };
    }

    public Scope getScope() {
        return scope;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }
}
