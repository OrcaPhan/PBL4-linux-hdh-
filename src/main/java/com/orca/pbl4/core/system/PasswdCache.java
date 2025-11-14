package com.orca.pbl4.core.system.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PasswdCache {
    private static final Path PASSWD = Paths.get("/etc/passwd");
    private static final PasswdCache INSTANCE = new PasswdCache();

    private volatile Map<Integer, String> uid2name = new ConcurrentHashMap<>();
    private volatile long lastMtime = -1L;

    private PasswdCache() {
        reloadIfNeeded();
    }

    public static PasswdCache get() {
        return INSTANCE;
    }

    public String usernameOf(int uid) {
        reloadIfNeeded();
        String n = uid2name.get(uid);
        if (n != null) return n;

        // Fallback: hỏi NSS
        String via = nssLookup(uid);
        if (via != null && !via.isBlank()) {
            uid2name.put(uid, via); // cache lại
            return via;
        }
        // Cuối cùng: trả về số
        return String.valueOf(uid);
    }

    private void reloadIfNeeded() {
        try {
            long m = Files.getLastModifiedTime(PASSWD).toMillis();
            if (m != lastMtime) {
                uid2name = loadPasswd();
                lastMtime = m;
            }
        } catch (IOException ignore) {
            // Không đọc được thì giữ map hiện tại
        }
    }
    private Map<Integer, String> loadPasswd() throws IOException {
        Map<Integer, String> map = new HashMap<>(256);
        try (BufferedReader br = Files.newBufferedReader(PASSWD)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                // name:passwd:uid:gid:gecos:home:shell
                String[] f = line.split(":", 7);
                if (f.length >= 3) {
                    try {
                        int uid = Integer.parseInt(f[2]);
                        map.put(uid, f[0]);
                    } catch (NumberFormatException ignore) {}
                }
            }
        }
        return map;
    }

    private static String nssLookup(int uid) {
        try {
            Process p = new ProcessBuilder("getent", "passwd", String.valueOf(uid))
                    .redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String out = br.readLine();
                p.waitFor();
                if (out != null && !out.isBlank()) {
                    String[] f = out.split(":", 7);
                    if (f.length >= 1) return f[0]; // username
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}
