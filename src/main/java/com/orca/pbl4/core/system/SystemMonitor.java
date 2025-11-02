package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.*;
import java.util.List;

public class SystemMonitor {
    private final Sampler sampler;


    public SystemMonitor() { this(new Sampler()); }
    public SystemMonitor(Sampler sampler) { this.sampler = sampler; }


    // --- Hàm lẻ ---
    public CpuInfo getCpuInfo() { return sampler.readCpu(); }
    public MemoryInfo getMemoryInfo() { return sampler.readMemory(); }
    public DiskInfo getDiskTotal() { return sampler.readDiskTotal(); }
    public NetworkInfo getNetTotal() { return sampler.readNetTotal(); }
    public List<ProcessInfo> getProcesses(boolean withCmdline) { return sampler.readProcesses(withCmdline); }


    // --- Gói đầy đủ ---
    public SystemSnapshot getSystemSnapshot(boolean withCmdline) { return sampler.readAll(withCmdline); }


    /** Không cần tài nguyên nền, close để tuân thủ try-with-resources nếu dùng */
//    @Override public void close() { /* no-op */ }
}
