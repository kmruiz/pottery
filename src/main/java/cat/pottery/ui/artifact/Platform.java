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
