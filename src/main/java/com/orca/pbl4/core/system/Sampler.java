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

    public DiskInfo readDiskTotal() { return diskReader.readTotal(); }

    public NetworkInfo readNetTotal() { return netReader.readTotal(); }

    public List<ProcessInfo> readProcesses(boolean withCmdline) {
        return procReader.readAll(withCmdline);
    }

    public SystemSnapshot readAll(boolean withCmdline) {
        CpuInfo cpu = readCpu();
        List<CpuInfo> perCoreCpus = cpuReader.readPerCore();
        MemoryInfo mem = readMemory();
        DiskInfo disk = readDiskTotal();
        NetworkInfo net = readNetTotal();
        List<ProcessInfo> procs = readProcesses(withCmdline);
        SystemSnapshot snapshot = new SystemSnapshot(cpu, mem, disk, net, procs, System.nanoTime());
        snapshot.setPerCoreCpus(perCoreCpus);
        return snapshot;
    }

    public ProcReader getProcReader() {
        return procReader;
    }
}
