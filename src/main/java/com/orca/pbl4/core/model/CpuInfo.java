package com.orca.pbl4.core.model;

public class CpuInfo {

    public final long user;
    public final long nice;
    public final long system;
    public final long idle;
    public final long iowait;
    public final long irq;
    public final long softirq;
    public final long steal;
    public final long guest;
    public final long guestNice;

    public final int coreCount;
    public float usagePercent;

    public CpuInfo(long user, long nice, long system, long idle, long iowait, long irq, long softirq, long steal, long guest, long guestNice, int coreCount) {
        this.user = user;
        this.nice = nice;
        this.system = system;
        this.idle = idle;
        this.iowait = iowait;
        this.irq = irq;
        this.softirq = softirq;
        this.steal = steal;
        this.guest = guest;
        this.guestNice = guestNice;
        this.coreCount = coreCount;
    }

    public long getTotal() {
        return user+ nice+ system+ idle+ iowait+ irq+ softirq+ steal+ guest+ guestNice;
    }

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
