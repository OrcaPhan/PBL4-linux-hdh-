package com.orca.pbl4.core.model;


public class CpuInfo {

    private long user;
    private long nice;
    private long system;
    private long idle;
    private long iowait;
    private long irq;
    private long softirq;
    private long steal;
    private long guest;
    private long guestNice;
    private int coreCount;

    public CpuInfo() {}

    public CpuInfo(long user, long nice, long system, long idle, long iowait,
                   long irq, long softirq, long steal, long guest, long guestNice, int coreCount) {
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


    public long getUser() { return user; }
    public void setUser(long user) { this.user = user; }

    public long getNice() { return nice; }
    public void setNice(long nice) { this.nice = nice; }

    public long getSystem() { return system; }
    public void setSystem(long system) { this.system = system; }

    public long getIdle() { return idle; }
    public void setIdle(long idle) { this.idle = idle; }

    public long getIowait() { return iowait; }
    public void setIowait(long iowait) { this.iowait = iowait; }

    public long getIrq() { return irq; }
    public void setIrq(long irq) { this.irq = irq; }

    public long getSoftirq() { return softirq; }
    public void setSoftirq(long softirq) { this.softirq = softirq; }

    public long getSteal() { return steal; }
    public void setSteal(long steal) { this.steal = steal; }

    public long getGuest() { return guest; }
    public void setGuest(long guest) { this.guest = guest; }

    public long getGuestNice() { return guestNice; }
    public void setGuestNice(long guestNice) { this.guestNice = guestNice; }

    public int getCoreCount() { return coreCount; }
    public void setCoreCount(int coreCount) { this.coreCount = coreCount; }


    public long getTotalTicks() {
        return user + nice + system + idle + iowait + irq + softirq + steal;
    }
}
