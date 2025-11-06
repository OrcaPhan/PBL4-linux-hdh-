package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.NetworkInfo;

import java.util.List;

public class NetDevReader {
    private final ProcFs proc;


    public NetDevReader() { this(new ProcFs()); }
    public NetDevReader(ProcFs proc) { this.proc = proc; }


    public NetworkInfo readTotal() {
        List<String> lines = proc.readLines("net", "dev");
        long rx = 0L, tx = 0L;
        for (String line : lines) {
            if (!line.contains(":")) continue; // bỏ 2 dòng header
// Ví dụ: eth0: 123 0 0 0 0 0 0 0 456 0 0 0 0 0 0 0
            String[] parts = line.split(":");
            if (parts.length != 2) continue;
            String data = parts[1].trim();
            String[] f = data.split("\\s+");
            if (f.length < 16) continue;
            try {
                rx += Long.parseLong(f[0]); // receive bytes
                tx += Long.parseLong(f[8]); // transmit bytes
            } catch (NumberFormatException ignored) {}
        }
        NetworkInfo n = new NetworkInfo(rx,tx);
        return n;
    }
}
