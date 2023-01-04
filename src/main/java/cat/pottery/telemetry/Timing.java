package cat.pottery.telemetry;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public String end(String id) {
        timings.computeIfPresent(id, (k, v) -> System.currentTimeMillis() - v);

        long millis = timings.getOrDefault(id, 0l);
        return BigDecimal.valueOf(millis)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP) + " seconds";
    }
}
