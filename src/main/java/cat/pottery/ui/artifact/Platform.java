package cat.pottery.ui.artifact;

import java.util.List;

public record Platform(
        String version,
        List<String> produces
) {
}
