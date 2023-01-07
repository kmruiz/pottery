/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cat.pottery.engine.toolchain;

import cat.pottery.telemetry.Log;

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

    static {
        String javaHome = System.getenv("JAVA_HOME");
        String graalvmHome = System.getenv("GRAALVM_HOME");
        String containerBuilder = System.getenv("CONTAINER_BUILDER");

        if (javaHome == null) {
            if (graalvmHome == null) {
                Log.getInstance().error("Neither JAVA_HOME or GRAALVM_HOME are defined. Make sure you have the JDK downloaded and installed.");
                Log.getInstance().info("You can download the latest Open JDK from Adoptium: https://adoptium.net/");
                System.exit(1);
            }

            Log.getInstance().warn("JAVA_HOME is not set up. We are defaulting to GRAALVM_HOME which is configured properly.");
        }

        if (containerBuilder == null) {
            Log.getInstance().warn("CONTAINER_BUILDER is not configured. Packaging a 'container' or 'docker' artifact won't work.");
        }
    }

    public static Toolchain systemDefault() {
        String javaHome = System.getenv("JAVA_HOME");
        String graalvmHome = System.getenv("GRAALVM_HOME");
        String containerBuilder = System.getenv("CONTAINER_BUILDER");

        if (javaHome == null) {
            if (graalvmHome == null) {
                System.exit(1);
            }

            javaHome = graalvmHome;
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
        return "0.3.2";
    }
}
