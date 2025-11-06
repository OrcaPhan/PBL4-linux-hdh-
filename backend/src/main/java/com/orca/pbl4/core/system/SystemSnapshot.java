package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.*;

import java.util.List;

public class SystemSnapshot {
    private CpuInfo cpu; // raw cpu ticks
    private MemoryInfo memory; // raw mem KB
    private DiskInfo diskTotal; // aggregate sectors (đơn giản hoá)
    private NetworkInfo netTotal; // aggregate rx/tx bytes (đơn giản hoá)
    private List<ProcessInfo> processes;// raw processes (không %)
    private long collectedAtNanos; // thời điểm lấy (System.nanoTime)


    public SystemSnapshot() {}


    public SystemSnapshot(CpuInfo cpu, MemoryInfo memory, DiskInfo diskTotal,
                          NetworkInfo netTotal, List<ProcessInfo> processes, long collectedAtNanos) {
        this.cpu = cpu; this.memory = memory; this.diskTotal = diskTotal; this.netTotal = netTotal;
        this.processes = processes; this.collectedAtNanos = collectedAtNanos;
    }


    public CpuInfo getCpu() { return cpu; }
    public void setCpu(CpuInfo cpu) { this.cpu = cpu; }


    public MemoryInfo getMemory() { return memory; }
    public void setMemory(MemoryInfo memory) { this.memory = memory; }


    public DiskInfo getDiskTotal() { return diskTotal; }
    public void setDiskTotal(DiskInfo diskTotal) { this.diskTotal = diskTotal; }


    public NetworkInfo getNetTotal() { return netTotal; }
    public void setNetTotal(NetworkInfo netTotal) { this.netTotal = netTotal; }


    public List<ProcessInfo> getProcesses() { return processes; }
    public void setProcesses(List<ProcessInfo> processes) { this.processes = processes; }


    public long getCollectedAtNanos() { return collectedAtNanos; }
    public void setCollectedAtNanos(long collectedAtNanos) { this.collectedAtNanos = collectedAtNanos; }
}
