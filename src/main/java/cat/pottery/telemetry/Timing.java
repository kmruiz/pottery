package cat.pottery.telemetry;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class Timing {
    private final ConcurrentHashMap<String, Long> timings;
    private static final Timing INSTANCE = new Timing();

    private Timing() {
        this.timings = new ConcurrentHashMap<>();
    }

    public static Timing getInstance() {
        return INSTANCE;
    }

    public void start(String id) {
        timings.put(id, System.currentTimeMillis());
    }

    public void end(String id) {
        timings.computeIfPresent(id, (k, v) -> System.currentTimeMillis() - v);
    }

    public Duration durationOf(String id) {
        return Duration.ofMillis(timings.getOrDefault(id, 0l));
    }
}
