package com.orca.pbl4.core.model;

public class CpuInfo {

    public long user;
    public long nice;
    public long system;
    public long idle;
    public long iowait;
    public long irq;
    public long softirq;
    public long steal;
    public long guest;
    public long guestNice;

    public int coreCount;
    public float usagePercent;

    // Tinhs %CPU su dung toan he thong tu 2 mau
    public void calcUsage(CpuInfo prev){
        if(prev == null){
            usagePercent = 0f;
            return;
        }

        long idleAll = idle + iowait;
        long idleAllPrev = prev.idle + prev.iowait;

        long nonIdle = user + nice + system + irq + softirq +steal + guest + guestNice;
        long nonIdlePrevPrev = prev.user + prev.nice + prev.system + prev.irq + prev.softirq + prev.steal + prev.guest + prev.guestNice;

        long total = idleAll + nonIdle;
        long totalPrev = idleAllPrev + nonIdlePrevPrev;

        long totald = total - totalPrev;
        long idled = idleAll - idleAllPrev;

        if( total <= 0) {
            usagePercent = 0f;
            return;
        }

        usagePercent = (float)(100.0*((totald-idled)/(double)totald));
    }

}
