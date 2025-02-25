package cat.pottery.engine.dependencies.maven;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MavenDependencyTest {
    @Test
    @Tags({ @Tag("unit") })
    void decides_last_version_when_a_range() {
        var dep = new MavenDependency("", "", "[1,2)", "", MavenDependency.Scope.RUNTIME, "", Optional.empty());
        assertEquals("2", dep.decidedVersion());
    }

    @Test
    @Tags({ @Tag("unit") })
    void decides_current_version_when_not_a_range() {
        var dep = new MavenDependency("", "", "1.2.3", "", MavenDependency.Scope.RUNTIME, "", Optional.empty());
        assertEquals("1.2.3", dep.decidedVersion());
    }

    @Test
    @Tags({ @Tag("unit") })
    void chooses_newest_version() {
        var older = new MavenDependency("", "", "1.2.3", "", MavenDependency.Scope.RUNTIME, "", Optional.empty());
        var newer = new MavenDependency("", "", "1.2.4", "", MavenDependency.Scope.RUNTIME, "", Optional.empty());

        var choosen = older.max(newer);
        assertEquals(newer.version(), choosen.version());
    }
}