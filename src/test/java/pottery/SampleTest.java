package pottery;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleTest {
    @Test
    @Tags({ @Tag("unit") })
    void sample() {
        assertTrue(false);
    }

    @Test
    @Tags({ @Tag("unit") })
    void samplework() {
        assertTrue(true);
    }
}
