package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.MemoryInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemInfoReader {
    private final ProcFs proc;


    public MemInfoReader() { this(new ProcFs()); }
    public MemInfoReader(ProcFs proc) { this.proc = proc; }


    public MemoryInfo read() {
        List<String> lines = proc.readLines("meminfo");
        Map<String, Long> map = new HashMap<>();
        for (String line : lines) {
// Ví dụ: MemTotal: 16266392 kB
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim();
            String rest = line.substring(colon + 1).trim();
            String num = rest.split("\\s+")[0];
            try { map.put(key, Long.parseLong(num)); } catch (NumberFormatException ignored) {}
        }
        long memTotal = map.getOrDefault("MemTotal", 0L);
        long memFree = map.getOrDefault("MemFree", 0L);
        long buffers = map.getOrDefault("Buffers", 0L);
        long memAvailable = map.getOrDefault("MemAvailable", 0L);
        long cached = map.getOrDefault("Cached", 0L);
        long swapTotal = map.getOrDefault("SwapTotal", 0L);
        long swapFree = map.getOrDefault("SwapFree", 0L);
        MemoryInfo m = new MemoryInfo();
        m.setTotalKB(memTotal);
        m.setUsedKB(memTotal-memAvailable);
        m.setCachedKB(cached);
        m.setAvailableKB(memAvailable);
//        m.setUsedKB(memTotal-memFree-buffers-cached);
        m.setSwapTotalKB(swapTotal);
        m.setSwapUsedKB(swapTotal-swapFree);
        return m;
    }
}
