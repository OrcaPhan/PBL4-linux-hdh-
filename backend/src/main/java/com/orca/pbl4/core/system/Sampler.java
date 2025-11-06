package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.*;
import com.orca.pbl4.core.system.reader.*;

import java.util.List;

public class Sampler {
    private final CpuStatReader cpuReader;
    private final MemInfoReader memReader;
    private final DiskStatReader diskReader;
    private final NetDevReader netReader;
    private final ProcReader procReader;


    public Sampler() {
        this(new CpuStatReader(), new MemInfoReader(), new DiskStatReader(), new NetDevReader(), new ProcReader());
    }


    public Sampler(CpuStatReader cpuReader,
                   MemInfoReader memReader,
                   DiskStatReader diskReader,
                   NetDevReader netReader,
                   ProcReader procReader) {
        this.cpuReader = cpuReader;
        this.memReader = memReader;
        this.diskReader = diskReader;
        this.netReader = netReader;
        this.procReader = procReader;
    }


    public CpuInfo readCpu() { return cpuReader.read(); }


    public MemoryInfo readMemory() { return memReader.read(); }


    /** Đọc tổng DiskInfo (aggregate sectors). Nếu cần per-device hãy mở rộng DiskReader + model. */
    public DiskInfo readDiskTotal() { return diskReader.readTotal(); }


    /** Đọc tổng NetworkInfo (aggregate bytes). Nếu cần per-iface hãy mở rộng NetReader + model. */
    public NetworkInfo readNetTotal() { return netReader.readTotal(); }


    /**
     * Đọc danh sách tiến trình (raw). withCmdline=false để nhanh hơn cho lần quét nền.
     * Khi UI mở chi tiết tiến trình, service có thể gọi lại ProcReader để lấy cmdline/threads/handles.
     */
    public List<ProcessInfo> readProcesses(boolean withCmdline) {
        return procReader.readAll(withCmdline);
    }


    /** Lấy đầy đủ một mẫu hệ thống (raw) tại thời điểm gọi. */
    public SystemSnapshot readAll(boolean withCmdline) {
        CpuInfo cpu = readCpu();
        MemoryInfo mem = readMemory();
        DiskInfo disk = readDiskTotal();
        NetworkInfo net = readNetTotal();
        List<ProcessInfo> procs = readProcesses(withCmdline);
        return new SystemSnapshot(cpu, mem, disk, net, procs, System.nanoTime());
    }
}
