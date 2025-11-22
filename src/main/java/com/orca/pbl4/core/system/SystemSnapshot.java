package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot của hệ thống tại một thời điểm.
 * Chứa raw data từ /proc (chưa tính %).
 */
public class SystemSnapshot {
    /** CPU tổng (từ dòng "cpu " trong /proc/stat) */
    private CpuInfo cpu;
    /** Danh sách CPU per-core (từ các dòng "cpu0", "cpu1", ... trong /proc/stat) */
    private List<CpuInfo> perCoreCpus;
    /** Memory info (từ /proc/meminfo) */
    private MemoryInfo memory;
    /** Disk tổng (aggregate sectors từ /proc/diskstats) */
    private DiskInfo diskTotal;
    /** Network tổng (aggregate rx/tx bytes từ /proc/net/dev) */
    private NetworkInfo netTotal;
    /** Danh sách tiến trình (raw, chưa tính %CPU) */
    private List<ProcessInfo> processes;
    /** Thời điểm lấy snapshot (System.nanoTime) */
    private long collectedAtNanos;


    public SystemSnapshot() {
        this.perCoreCpus = new ArrayList<>();
    }


    public SystemSnapshot(CpuInfo cpu, MemoryInfo memory, DiskInfo diskTotal,
                          NetworkInfo netTotal, List<ProcessInfo> processes, long collectedAtNanos) {
        this.cpu = cpu;
        this.memory = memory;
        this.diskTotal = diskTotal;
        this.netTotal = netTotal;
        this.processes = processes;
        this.collectedAtNanos = collectedAtNanos;
        this.perCoreCpus = new ArrayList<>();
    }


    public CpuInfo getCpu() { return cpu; }
    public void setCpu(CpuInfo cpu) { this.cpu = cpu; }

    /**
     * Lấy danh sách CPU per-core.
     * @return Danh sách CpuInfo, mỗi phần tử là một core (theo thứ tự cpu0, cpu1, ...)
     */
    public List<CpuInfo> getPerCoreCpus() { return perCoreCpus; }
    public void setPerCoreCpus(List<CpuInfo> perCoreCpus) { 
        this.perCoreCpus = perCoreCpus != null ? perCoreCpus : new ArrayList<>();
    }


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
