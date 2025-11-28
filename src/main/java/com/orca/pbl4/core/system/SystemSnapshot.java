package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.*;

import java.util.ArrayList;
import java.util.List;

public class SystemSnapshot {
    private CpuInfo cpu;
    private List<CpuInfo> perCoreCpus;
    private MemoryInfo memory;
    private DiskInfo diskTotal;
    private NetworkInfo netTotal;
    private List<ProcessInfo> processes;
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
