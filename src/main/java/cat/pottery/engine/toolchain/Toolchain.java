package cat.pottery.engine.toolchain;

import java.nio.file.Path;

public final class Toolchain {
    private final Path toolchainPath;
    private final Path graalvmPath;
    private final Path containerBuilder;

    public Toolchain(Path toolchainPath, Path graalvmPath, Path containerBuilder) {
        this.toolchainPath = toolchainPath;
        this.graalvmPath = graalvmPath;
        this.containerBuilder = containerBuilder;
    }

    public static Toolchain systemDefault() {
        String javaHome = System.getenv("JAVA_HOME");
        String graalvmHome = System.getenv("GRAALVM_HOME");
        String containerBuilder = System.getenv("CONTAINER_BUILDER");

        if (javaHome == null) {
            javaHome = graalvmHome;
        }

        if (javaHome == null) {
            throw new Error();
        }

        return new Toolchain(Path.of(javaHome), graalvmHome == null ? null : Path.of(graalvmHome), containerBuilder == null ? null : Path.of(containerBuilder));
    }

    public Path javac() {
        return toolchainPath.resolve("bin").resolve("javac");
    }
    public Path graalvmNative() {
        return graalvmPath.resolve("bin").resolve("native-image");
    }
    public Path containerBuilder() {
        return containerBuilder;
    }

    public String javacVersion() {
        try {
            var process = Runtime.getRuntime().exec(new String[] { javac().toString(), "-version"});
            process.waitFor();

            return new String(process.getInputStream().readAllBytes()).split("\n")[0];
        } catch (Exception e) {
            return "unknown-version";
        }
    }

    public String currentUser() {
        return System.getProperty("user.name");
    }

    public String potteryVersion() {
        return "0.0.2";
    }
}
