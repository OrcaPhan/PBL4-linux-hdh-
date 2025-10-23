package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.MemoryInfo;
import com.orca.pbl4.core.model.ProcessInfo;

import java.util.List;

public interface Sampler {
    MemoryInfo readMemory();
    List<ProcessInfo> readProcesses();
    long readTotalCpuStick();
    int readLogicalCpuCount();
    long readPagesSizeKB();
}
