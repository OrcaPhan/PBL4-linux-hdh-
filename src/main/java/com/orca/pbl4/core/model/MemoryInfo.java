package com.orca.pbl4.core.model;

public class MemoryInfo {
    private long totalKB;
    private long usedKB;
    private long swapTotalKB;
    private long swapUsedKB;

    public float readMemInfo(){
        return totalKB == 0 ? 0f : (100f*(usedKB/(float)totalKB));
    }

}
