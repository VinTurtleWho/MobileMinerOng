package com.mobileminerong.diagnostic;

import com.mobileminerong.debug.DebugLogger;

public class DiagnosticManager {

    private static long lastReportTime = 0;

    // 1 second between full reports
    private static final long REPORT_INTERVAL = 1000;

    public static void report(String category, String message) {

        long now = System.currentTimeMillis();

        if (now - lastReportTime >= REPORT_INTERVAL) {
            DebugLogger.info(category, message);
            lastReportTime = now;
        }
    }


    public static void debug(String category, String message) {
        DebugLogger.debug(category, message);
    }


    public static void event(String message) {
        DebugLogger.info("EVENT", message);
    }


    public static void warn(String message) {
        DebugLogger.warn("WARNING", message);
    }


    public static void error(String message) {
        DebugLogger.error("ERROR", message);
    }
}
