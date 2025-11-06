package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.CpuInfo;

import java.util.List;

public class CpuStatReader {
    private final ProcFs proc;

    public CpuStatReader(){
        this(new ProcFs());
    }

    public CpuStatReader(ProcFs proc) {
        this.proc = proc;
    }

    public CpuInfo read(){
        List<String> lines = proc.readLines("stat");
        String cpuLine = null;
        int coreCount = 0;

        for(String line : lines){
            if(line.startsWith("cpu ")){
                cpuLine = line;
            }
            else if(line.startsWith("cpu")){coreCount++; }
        }
        if(cpuLine == null){
            throw new IllegalStateException("/proc/stat missing 'cpu' line");
        }
        String[] p = cpuLine.trim().split("\\s+");


        long user = parseLongSafe(p,1), nice=parseLongSafe(p,2), system=parseLongSafe(p,3), idle=parseLongSafe(p,4),
                iowait=parseLongSafe(p,5), irq=parseLongSafe(p,6), softirq=parseLongSafe(p,7), steal=parseLongSafe(p,8),
                guest=parseLongSafe(p,9), guestNice=parseLongSafe(p,10);


        CpuInfo info = new CpuInfo();
        info.setUser(user);
        info.setNice(nice);
        info.setSystem(system);
        info.setIdle(idle);
        info.setIowait(iowait);
        info.setIrq(irq);
        info.setSoftirq(softirq);
        info.setSteal(steal);
        info.setGuest(guest);
        info.setGuestNice(guestNice);
        info.setCoreCount(coreCount);
        return info;
    }
    private static long parseLongSafe(String[] arr, int idx) {
        return (idx < arr.length) ? Long.parseLong(arr[idx]) : 0L;
    }
}
