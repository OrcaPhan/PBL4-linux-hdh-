package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.DiskInfo;

import java.util.List;

public class DiskStatReader {
    private final ProcFs proc;


    public DiskStatReader() { this(new ProcFs()); }
    public DiskStatReader(ProcFs proc) { this.proc = proc; }


    public DiskInfo readTotal() {
// /proc/diskstats: fields… reads completed, reads merged, sectors read, time reading, writes completed, writes merged, sectors written, time writing, ...
        List<String> lines = proc.readLines("diskstats");
        long sectorsRead = 0, sectorsWritten = 0;
        for (String line : lines) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 14) continue; // bảo thủ
// Theo kernel docs: idx 5 = sectors read, idx 9 = sectors written (tính từ 0 sau khi bỏ major,minor,dev)
// Cụ thể: [0]=major [1]=minor [2]=name [3]=reads_completed [4]=reads_merged [5]=sectors_read [6]=time_reading_ms [7]=writes_completed [8]=writes_merged [9]=sectors_written [10]=time_writing_ms ...
            try {
                sectorsRead += Long.parseLong(p[5]);
                sectorsWritten += Long.parseLong(p[9]);
            } catch (NumberFormatException ignored) {}
        }
        DiskInfo d = new DiskInfo(sectorsRead,sectorsWritten);
        return d;
    }
}
