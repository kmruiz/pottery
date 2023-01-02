package cat.pottery.telemetry;

import com.github.tomaslanger.chalk.Chalk;

public final class Log {
    private Log() {}

    private static final Log INSTANCE = new Log();

    public static Log getInstance() {
        return INSTANCE;
    }

    public synchronized void info(String fmt, Object ...params) {
        System.out.print(Chalk.on("[INFO]  ").blue());
        System.out.printf((fmt) + "%n", params);
    }

    public synchronized void warn(String fmt, Object ...params) {
        System.out.print(Chalk.on("[WARN]  ").yellow());
        System.out.printf((fmt) + "%n", params);
    }

    public synchronized void error(String fmt, Object ...params) {
        System.err.print(Chalk.on("[ERROR] ").red());
        System.err.printf((fmt) + "%n", params);
    }

    public synchronized void error(String fmt, Throwable ex, Object ...params) {
        System.err.print(Chalk.on("[ERROR] ").red());
        System.err.printf((fmt) + "%n", params);
        ex.printStackTrace(System.err);
    }

    public synchronized void fatal(String fmt, Throwable ex, Object ...params) {
        System.err.print(Chalk.on("[ERROR] ").bgRed().white());
        System.err.printf((fmt) + "%n", params);
        ex.printStackTrace(System.err);
    }
}
