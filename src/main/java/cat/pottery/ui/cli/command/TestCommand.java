package cat.pottery.ui.cli.command;

import cat.pottery.engine.compiler.IncrementalCompiler;
import cat.pottery.engine.dependencies.DependencyResolver;
import cat.pottery.engine.dependencies.DownloadManager;
import cat.pottery.engine.dependencies.PomContextRegistry;
import cat.pottery.engine.dependencies.maven.MavenDependency;
import cat.pottery.engine.toolchain.Toolchain;
import cat.pottery.telemetry.Log;
import cat.pottery.telemetry.Timing;
import cat.pottery.ui.parser.YamlArtifactFileParser;
import cat.pottery.ui.parser.result.ArtifactFileParserResult;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class TestCommand implements CliCommand {
    private static final String TIMING_ID = "test-command";

    @Override
    public void execute(CommandLine.ParseResult parseResult) {
        Timing.getInstance().start(TIMING_ID);
        var queue = new ArrayBlockingQueue<MavenDependency>(128);
        var manager = new DownloadManager(queue, new ConcurrentHashMap<>(32, 1.2f, 4), 4, true);
        var pomContextRegistry = new PomContextRegistry(new ConcurrentHashMap<>(32, 1.2f, 4));

        var dependencyResolver = new DependencyResolver(manager, queue, 4, pomContextRegistry);
        var artifactDoc = (ArtifactFileParserResult.Success) new YamlArtifactFileParser().parse(Path.of("pottery.yaml"));

        var deps = dependencyResolver.downloadDependenciesOfArtifact(artifactDoc.document());
        var compiler = new IncrementalCompiler(Toolchain.systemDefault());
        var targetClassesPath = Path.of("target", "classes");
        var targetTestClassesPath = Path.of("target", "test-classes");

        var process = compiler.compileTree(artifactDoc.document(), Path.of("src", "main", "java").toAbsolutePath(), targetClassesPath.toAbsolutePath(), deps);

        if (process != null) {
            try {
                process.waitFor();
            } catch (InterruptedException e) {

            }
        }

        process = compiler.compileTree(artifactDoc.document(), Path.of("src", "test", "java").toAbsolutePath(), targetTestClassesPath.toAbsolutePath(), deps);

        URLClassLoader cl = null;
        try {
            cl = new URLClassLoader(new URL[] {
                    targetClassesPath.toUri().toURL(),
                    targetTestClassesPath.toUri().toURL(),
            });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Thread.currentThread().setContextClassLoader(cl);
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(allPackageSelector(targetTestClassesPath))
                .filters(includeClassNamePatterns(".*Test"))
                .build();
        Launcher launcher = LauncherFactory.create(LauncherConfig.builder()
                .build());
        launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        boolean failed = showTestResults(listener.getSummary(), parseResult);
        var duration = Timing.getInstance().end(TIMING_ID);
        Log.getInstance().info("Tests ran in %s.", duration);

        if (failed && !WatchCommand.IS_WATCHING) {
            System.exit(1);
        }
    }

    private boolean showTestResults(TestExecutionSummary summary, CommandLine.ParseResult parseResult) {
        var log = Log.getInstance();
        var failed = false;

        if (summary.getTestsFailedCount() > 0) {
            failed = true;

            log.error("%d failed tests.", summary.getTestsFailedCount());
            for (var failure : summary.getFailures()) {
                var tags = failure.getTestIdentifier().getTags().stream().map(TestTag::getName).sorted().collect(Collectors.joining(", "));

                if (parseResult.hasMatchedOption('v')) {
                    log.error("(%s) %s: %s", failure.getException(), tags, failure.getTestIdentifier().getDisplayName(), failure.getException().getMessage());
                } else {
                    log.error("(%s) %s: %s", tags, failure.getTestIdentifier().getDisplayName(), failure.getException().getMessage());
                }
            }
        }

        if (summary.getTestsSkippedCount() > 0) {
            if (parseResult.hasMatchedOption('x')) {
                log.error("%d skipped tests. Failing due to strict mode.", summary.getTestsSkippedCount());
                failed = true;
            } else {
                log.warn("%d skipped tests.", summary.getTestsSkippedCount());
            }
        }

        if (summary.getTestsSucceededCount() == 0) {
            log.info("0 of %d (0%%) successful tests.", summary.getTestsStartedCount());
        } else {
            var successPercentage = BigDecimal.valueOf(summary.getTestsSucceededCount())
                    .setScale(1)
                    .divide(BigDecimal.valueOf(summary.getTestsStartedCount()), RoundingMode.HALF_DOWN)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_DOWN) + "%";

            log.info("%d of %d (%s) successful tests.",
                    summary.getTestsSucceededCount(),
                    summary.getTestsStartedCount(),
                    successPercentage
            );
        }

        return failed;
    }

    private List<? extends DiscoverySelector> allPackageSelector(Path testFolder) {
        return Arrays.stream(testFolder.toFile().listFiles(File::isDirectory)).sorted()
                .map(File::getName)
                .map(DiscoverySelectors::selectPackage)
                .toList();

    }
}
