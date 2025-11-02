package com.orca.pbl4.core.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Sysconf {

    public static long pageSizeKBOrDefault(long defKB) {
        String out = run("getconf", "PAGESIZE");
        if (out != null) {
            try {
                long bytes = Long.parseLong(out.trim());
                return Math.max(1, bytes / 1024);
            } catch (NumberFormatException ignored) {}
        }
        return defKB;
    }

    public static long clkTckOrDefault(long def) {
        String out = run("getconf", "CLK_TCK");
        if (out != null) {
            try {
                return Long.parseLong(out.trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static String run(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String s = br.readLine();
                p.waitFor();
                return s;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
