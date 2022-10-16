package cat.pottery.engine.toolchain;

import java.nio.file.Path;

public final class Toolchain {
    private final Path toolchainPath;

    public Toolchain(Path toolchainPath) {
        this.toolchainPath = toolchainPath;
    }

    public static Toolchain systemDefault() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = System.getenv("GRAALVM_HOME");
        }

        if (javaHome == null) {
            throw new Error();
        }

        return new Toolchain(Path.of(javaHome));
    }

    public Path javac() {
        return toolchainPath.resolve("bin").resolve("javac");
    }
}
