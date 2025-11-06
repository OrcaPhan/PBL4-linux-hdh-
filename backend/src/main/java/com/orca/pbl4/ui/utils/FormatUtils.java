package com.orca.pbl4.ui.utils;

import java.util.Locale;

/**
 * Utility class cho formatting data
 */
public class FormatUtils {
    
    /**
     * Format KB to human readable format (KB, MB, GB, TB)
     */
    public static String formatKB(long kb) {
        double v = kb;
        String[] units = {"KB", "MB", "GB", "TB"};
        int u = 0;
        while (v >= 1024 && u < units.length - 1) {
            v /= 1024;
            u++;
        }
        return String.format(Locale.US, "%.1f %s", v, units[u]);
    }
    
    /**
     * Format bytes to human readable format
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        return formatKB(bytes / 1024);
    }
    
    /**
     * Format percentage with 2 decimal places
     */
    public static String formatPercent(double percent) {
        return String.format(Locale.US, "%.2f%%", percent);
    }
    
    /**
     * Safe get long value (return 0 if null)
     */
    public static long safeLong(Long value) {
        return value != null ? value : 0L;
    }
    
    /**
     * Safe get string value (return empty string if null)
     */
    public static String safeString(String value) {
        return value != null ? value : "";
    }
}
