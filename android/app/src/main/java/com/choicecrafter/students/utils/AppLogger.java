package com.choicecrafter.students.utils;

import android.os.SystemClock;
import android.util.Log;

import com.choicecrafter.students.BuildConfig;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized logger that enriches log statements with timestamps, thread information and
 * structured key/value pairs. It helps us understand what the application is doing at every step
 * without having to sprinkle manual formatting across the code base.
 */
public final class AppLogger {

    private static final String GLOBAL_TAG = "StudentApp";
    private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    );

    private static volatile boolean forceLogging = false;

    private AppLogger() {
        // Utility class.
    }

    /**
     * Forces logs to be printed even in non-debug builds. Useful for diagnosing issues in release
     * builds when attaching a debugger is not possible.
     */
    public static void setForceLogging(boolean enabled) {
        forceLogging = enabled;
    }

    public static TraceSession trace(String component, String stepName, Object... keyValues) {
        logInternal(Log.DEBUG, component, "▶ " + stepName + " started", null, keyValues);
        return new TraceSession(component, stepName);
    }

    public static void d(String component, String message, Object... keyValues) {
        logInternal(Log.DEBUG, component, message, null, keyValues);
    }

    public static void d(String component, String message, Throwable throwable, Object... keyValues) {
        logInternal(Log.DEBUG, component, message, throwable, keyValues);
    }

    public static void i(String component, String message, Object... keyValues) {
        logInternal(Log.INFO, component, message, null, keyValues);
    }

    public static void w(String component, String message, Object... keyValues) {
        logInternal(Log.WARN, component, message, null, keyValues);
    }

    public static void w(String component, String message, Throwable throwable, Object... keyValues) {
        logInternal(Log.WARN, component, message, throwable, keyValues);
    }

    public static void e(String component, String message, Object... keyValues) {
        logInternal(Log.ERROR, component, message, null, keyValues);
    }

    public static void e(String component, String message, Throwable throwable, Object... keyValues) {
        logInternal(Log.ERROR, component, message, throwable, keyValues);
    }

    private static void logInternal(int priority, String component, String message, Throwable throwable,
                                    Object... keyValues) {
        if (!shouldLog(priority)) {
            return;
        }

        String formattedMessage = buildMessage(component, message, keyValues);
        if (throwable != null) {
            Log.println(priority, GLOBAL_TAG, formattedMessage + '\n' + Log.getStackTraceString(throwable));
        } else {
            Log.println(priority, GLOBAL_TAG, formattedMessage);
        }
    }

    private static boolean shouldLog(int priority) {
        return BuildConfig.DEBUG || forceLogging || priority >= Log.WARN;
    }

    private static String buildMessage(String component, String message, Object... keyValues) {
        StringBuilder builder = new StringBuilder();
        builder.append('[')
                .append(TIMESTAMP_FORMAT.get().format(new Date()))
                .append("] [")
                .append(Thread.currentThread().getName())
                .append(']');
        if (component != null && !component.isEmpty()) {
            builder.append(' ').append('[').append(component).append(']');
        }
        builder.append(' ').append(message);

        if (keyValues != null && keyValues.length > 0) {
            if (keyValues.length % 2 != 0) {
                builder.append(" | data=").append(Arrays.toString(keyValues));
            } else {
                builder.append(" | ");
                for (int i = 0; i < keyValues.length; i += 2) {
                    Object key = keyValues[i];
                    Object value = keyValues[i + 1];
                    builder.append(key).append('=').append(value);
                    if (i + 2 < keyValues.length) {
                        builder.append(", ");
                    }
                }
            }
        }

        return builder.toString();
    }

    public static final class TraceSession implements AutoCloseable {
        private final String component;
        private final String stepName;
        private final long startTimeMs;
        private boolean closed;

        private TraceSession(String component, String stepName) {
            this.component = component;
            this.stepName = stepName;
            this.startTimeMs = SystemClock.elapsedRealtime();
        }

        public void closeWithError(Throwable throwable, Object... keyValues) {
            if (closed) {
                return;
            }
            closed = true;
            long duration = SystemClock.elapsedRealtime() - startTimeMs;
            Object[] extended = append(keyValues, "durationMs", duration);
            logInternal(Log.ERROR, component, "✖ " + stepName + " failed", throwable, extended);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            long duration = SystemClock.elapsedRealtime() - startTimeMs;
            logInternal(Log.DEBUG, component, "✔ " + stepName + " finished", null, "durationMs", duration);
        }

        private Object[] append(Object[] keyValues, Object key, Object value) {
            if (keyValues == null) {
                return new Object[]{key, value};
            }
            Object[] extended = Arrays.copyOf(keyValues, keyValues.length + 2);
            extended[keyValues.length] = key;
            extended[keyValues.length + 1] = value;
            return extended;
        }
    }
}
