package com.mobileminerong.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
    private static BufferedWriter writer;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        try {
            File dir = new File("logs/mobileminerong");
            if (!dir.exists()) dir.mkdirs();

            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            File logFile = new File(dir, "session_" + timestamp + ".log");

            writer = new BufferedWriter(new FileWriter(logFile, true));
            initialized = true;
            log("INFO", "SYSTEM", "=== MobileMinerOng Log Session Started ===");
        } catch (IOException e) {
            System.err.println("[MobileMinerOng] Failed to initialize DebugLogger: " + e.getMessage());
        }
    }

    public static synchronized void log(String level, String category, String message) {
        if (!initialized) init();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        String line = String.format("[%s] [%s] [%s] %s", time, level, category, message);

        // Mirror output to stdout/console
        System.out.println("[MobileMinerOng] " + line);

        // Persist to file buffer
        if (writer != null) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush(); // Immediate flush to ensure log persistence on crash
            } catch (IOException ignored) {}
        }
    }

    public static void info(String category, String message) { log("INFO", category, message); }
    public static void debug(String category, String message) { log("DEBUG", category, message); }
    public static void warn(String category, String message) { log("WARN", category, message); }
    public static void error(String category, String message) { log("ERROR", category, message); }

    public static synchronized void close() {
        if (!initialized) return;
        log("INFO", "SYSTEM", "=== MobileMinerOng Log Session Ending ===");
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {}
        }
        initialized = false;
    }
}
