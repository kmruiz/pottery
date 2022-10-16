package cat.pottery.engine.toolchain;

import java.io.IOException;
import java.nio.file.Path;

public final class Toolchain {
    private final Path toolchainPath;
    private final Path graalvmPath;

    public Toolchain(Path toolchainPath, Path graalvmPath) {
        this.toolchainPath = toolchainPath;
        this.graalvmPath = graalvmPath;
    }

    public static Toolchain systemDefault() {
        String javaHome = System.getenv("JAVA_HOME");
        String graalvmHome = System.getenv("GRAALVM_HOME");

        if (javaHome == null) {
            javaHome = graalvmHome;
        }

        if (javaHome == null) {
            throw new Error();
        }

        return new Toolchain(Path.of(javaHome), Path.of(graalvmHome));
    }

    public Path javac() {
        return toolchainPath.resolve("bin").resolve("javac");
    }
    public Path graalvmNative() {
        return graalvmPath.resolve("bin").resolve("native-image");
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
        return "0.0.1";
    }
}
