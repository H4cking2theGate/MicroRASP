package com.h2tg.rasp.log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Lightweight logger using java.util.logging for RASP agent.
 *
 * Features:
 * - Uses Java native logging (no external dependencies)
 * - Outputs to both file and console
 * - Thread-safe and minimal overhead
 * - Auto-creates log directory
 *
 * Log Location:
 * - Default: ${user.home}/rasp-logs/microrasp.log
 * - Can be overridden via system property: rasp.log.path
 */
public class MicroLogger {

    private static final Logger LOGGER = Logger.getLogger("com.h2tg.rasp");
    private static final String LOG_DIR_PROPERTY = "rasp.log.path";
    private static final String DEFAULT_LOG_DIR = "rasp-logs";
    private static volatile boolean initialized = false;

    /**
     * Initialize the logger with file handler.
     * This is called automatically on first use, but can be called explicitly.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            // Disable parent handlers to avoid duplicate console output
            LOGGER.setUseParentHandlers(false);

            // Set logging level
            LOGGER.setLevel(Level.ALL);

            // Setup console handler for immediate feedback
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new CompactFormatter());
            LOGGER.addHandler(consoleHandler);

            // Setup file handler for persistent logging
            String logDir = System.getProperty(LOG_DIR_PROPERTY, DEFAULT_LOG_DIR);
            File logDirFile = new File(logDir);

            // Create log directory if it doesn't exist
            if (!logDirFile.exists()) {
                if (!logDirFile.mkdirs()) {
                    System.err.println("[MicroRASP] Warning: Could not create log directory: " + logDir);
                    // Continue without file logging
                    initialized = true;
                    return;
                }
            }

            // Log file path with timestamp
            String logFileName = logDir + File.separator + "microrasp.log";

            // FileHandler with 10MB limit, 5 rotating files, append mode
            FileHandler fileHandler = new FileHandler(logFileName, 10 * 1024 * 1024, 5, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new DetailedFormatter());
            LOGGER.addHandler(fileHandler);

            initialized = true;

            LOGGER.info("MicroRASP logger initialized successfully. Log file: " + logFileName);
        } catch (IOException e) {
            System.err.println("[MicroRASP] Failed to initialize file logging: " + e.getMessage());
            e.printStackTrace();
            // Continue with console-only logging
            initialized = true;
        }
    }

    /**
     * Ensure logger is initialized before use
     */
    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    /**
     * Log info message with category
     *
     * @param category Log category (e.g., "Agent", "HookRegistry")
     * @param message Log message
     */
    public static void info(String category, String message) {
        ensureInitialized();
        LOGGER.log(Level.INFO, formatMessage(category, message));
    }

    /**
     * Log warning message with category
     *
     * @param category Log category
     * @param message Log message
     */
    public static void warn(String category, String message) {
        ensureInitialized();
        LOGGER.log(Level.WARNING, formatMessage(category, message));
    }

    /**
     * Log error message with category
     *
     * @param category Log category
     * @param message Log message
     */
    public static void error(String category, String message) {
        ensureInitialized();
        LOGGER.log(Level.SEVERE, formatMessage(category, message));
    }

    /**
     * Log error message with category and exception
     *
     * @param category Log category
     * @param message Log message
     * @param throwable Exception to log
     */
    public static void error(String category, String message, Throwable throwable) {
        ensureInitialized();
        LOGGER.log(Level.SEVERE, formatMessage(category, message), throwable);
    }

    /**
     * Log debug message with category (only logged to file)
     *
     * @param category Log category
     * @param message Log message
     */
    public static void debug(String category, String message) {
        ensureInitialized();
        LOGGER.log(Level.FINE, formatMessage(category, message));
    }

    /**
     * Format message with category prefix
     */
    private static String formatMessage(String category, String message) {
        return "[" + category + "] " + message;
    }

    /**
     * Compact formatter for console output
     */
    private static class CompactFormatter extends Formatter {
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();

            // Timestamp
            sb.append(dateFormat.format(new Date(record.getMillis())));
            sb.append(" ");

            // Level
            sb.append(getLevelShort(record.getLevel()));
            sb.append(" ");

            // Message
            sb.append(formatMessage(record));
            sb.append("\n");

            return sb.toString();
        }

        private String getLevelShort(Level level) {
            if (level == Level.SEVERE) return "[ERROR]";
            if (level == Level.WARNING) return "[WARN ]";
            if (level == Level.INFO) return "[INFO ]";
            return "[DEBUG]";
        }
    }

    /**
     * Detailed formatter for file output
     */
    private static class DetailedFormatter extends Formatter {
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();

            // Timestamp
            sb.append(dateFormat.format(new Date(record.getMillis())));
            sb.append(" ");

            // Thread
            sb.append("[Thread-").append(Thread.currentThread().getName()).append("] ");

            // Level
            sb.append(String.format("%-7s", record.getLevel().getName()));
            sb.append(" ");

            // Logger name (compact)
            String loggerName = record.getLoggerName();
            if (loggerName != null && loggerName.length() > 30) {
                loggerName = "..." + loggerName.substring(loggerName.length() - 27);
            }
            sb.append(String.format("%-30s", loggerName));
            sb.append(" - ");

            // Message
            sb.append(formatMessage(record));
            sb.append("\n");

            // Exception (if present)
            if (record.getThrown() != null) {
                sb.append(getStackTrace(record.getThrown()));
            }

            return sb.toString();
        }

        private String getStackTrace(Throwable throwable) {
            StringBuilder sb = new StringBuilder();
            sb.append(throwable.getClass().getName());
            sb.append(": ");
            sb.append(throwable.getMessage());
            sb.append("\n");

            for (StackTraceElement element : throwable.getStackTrace()) {
                sb.append("    at ");
                sb.append(element.toString());
                sb.append("\n");
            }

            if (throwable.getCause() != null) {
                sb.append("Caused by: ");
                sb.append(getStackTrace(throwable.getCause()));
            }

            return sb.toString();
        }
    }

    /**
     * Shutdown logger and flush all handlers
     */
    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        for (Handler handler : LOGGER.getHandlers()) {
            handler.flush();
            handler.close();
        }

        initialized = false;
    }
}
