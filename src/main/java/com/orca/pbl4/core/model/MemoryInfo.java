package com.orca.pbl4.core.model;

public class MemoryInfo {
    private long totalKB;
    private long usedKB;
    private long cachedKB;
    private long swapTotalKB;
    private long swapUsedKB;

    public MemoryInfo() {}
    public MemoryInfo( long totalKB, long usedKB, long cachedKB , long swapTotalKB, long swapUsedKB) {
        this.totalKB = totalKB;
        this.usedKB = usedKB;
        this.cachedKB = cachedKB;
        this.swapTotalKB = swapTotalKB;
        this.swapUsedKB = swapUsedKB;
    }

    public long getTotalKB() {
        return totalKB;
    }
    public void setTotalKB(long totalKB) {
        this.totalKB = totalKB;
    }
    public long getUsedKB() {
        return usedKB;
    }
    public void setUsedKB(long usedKB) {
        this.usedKB = usedKB;
    }
    public long getCachedKB() {return cachedKB; }
    public void setCachedKB(long cachedKB) {this.cachedKB = cachedKB;}
    public long getSwapTotalKB() {
        return swapTotalKB;
    }
    public void setSwapTotalKB(long swapTotalKB) {
        this.swapTotalKB = swapTotalKB;
    }
    public long getSwapUsedKB() {
        return swapUsedKB;
    }
    public void setSwapUsedKB(long swapUsedKB) {
        this.swapUsedKB = swapUsedKB;
    }

}
