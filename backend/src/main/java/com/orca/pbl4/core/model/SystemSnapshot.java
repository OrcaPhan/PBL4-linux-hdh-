package com.orca.pbl4.core.model;

import java.util.List;

/**
 * Snapshot của toàn bộ hệ thống tại một thời điểm
 */
public class SystemSnapshot {
    private CpuInfo cpu;
    private MemoryInfo memory;
    private DiskInfo disk;
    private NetworkInfo network;
    private List<ProcessInfo> processes;
    private long timestamp;

    public SystemSnapshot() {
        this.timestamp = System.currentTimeMillis();
    }

    public SystemSnapshot(CpuInfo cpu, MemoryInfo memory, DiskInfo disk, 
                          NetworkInfo network, List<ProcessInfo> processes, long timestamp) {
        this.cpu = cpu;
        this.memory = memory;
        this.disk = disk;
        this.network = network;
        this.processes = processes;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public CpuInfo getCpu() {
        return cpu;
    }

    public void setCpu(CpuInfo cpu) {
        this.cpu = cpu;
    }

    public MemoryInfo getMemory() {
        return memory;
    }

    public void setMemory(MemoryInfo memory) {
        this.memory = memory;
    }

    public DiskInfo getDisk() {
        return disk;
    }

    public void setDisk(DiskInfo disk) {
        this.disk = disk;
    }

    public NetworkInfo getNetwork() {
        return network;
    }

    public void setNetwork(NetworkInfo network) {
        this.network = network;
    }

    public List<ProcessInfo> getProcesses() {
        return processes;
    }

    public void setProcesses(List<ProcessInfo> processes) {
        this.processes = processes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
