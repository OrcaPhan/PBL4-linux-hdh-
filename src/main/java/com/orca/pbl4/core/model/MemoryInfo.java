package com.orca.pbl4.core.model;

public class MemoryInfo {
    private final long totalKB;
    private final long usedKB;
    private final long swapTotalKB;
    private final long swapUsedKB;

    public MemoryInfo( long totalKB, long usedKB, long swapTotalKB, long swapUsedKB) {
        this.totalKB = totalKB;
        this.usedKB = usedKB;
        this.swapTotalKB = swapTotalKB;
        this.swapUsedKB = swapUsedKB;
    }

    public float readMemInfo(){
        return totalKB == 0 ? 0f : (100f*(usedKB/(float)totalKB));
    }

}
