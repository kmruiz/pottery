package cat.pottery.ui.artifact;

public record Manifest(String mainClass) {
    public String getMainClass() {
        return mainClass;
    }
}
