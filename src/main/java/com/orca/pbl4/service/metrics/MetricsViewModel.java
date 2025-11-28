package com.orca.pbl4.service.metrics;

public class MetricsViewModel {
    
    // Memory
    private final float memPercent;
    private final String memPercentText;
    
    // Swap
    private final boolean swapAvailable;
    private final float swapPercent;
    private final String swapText;
    
    // Network
    private final float rxKbps;
    private final float txKbps;
    private final String rxRateText;
    private final String txRateText;


    public MetricsViewModel(float memPercent, String memPercentText,
                           boolean swapAvailable, float swapPercent, String swapText,
                           float rxKbps, float txKbps, String rxRateText, String txRateText) {
        this.memPercent = memPercent;
        this.memPercentText = memPercentText;
        this.swapAvailable = swapAvailable;
        this.swapPercent = swapPercent;
        this.swapText = swapText;
        this.rxKbps = rxKbps;
        this.txKbps = txKbps;
        this.rxRateText = rxRateText;
        this.txRateText = txRateText;
    }

    public float getMemPercent() {
        return memPercent;
    }

    public String getMemPercentText() {
        return memPercentText;
    }

    public boolean isSwapAvailable() {
        return swapAvailable;
    }

    public float getSwapPercent() {
        return swapPercent;
    }

    public String getSwapText() {
        return swapText;
    }

    public float getRxKbps() {
        return rxKbps;
    }

    public float getTxKbps() {
        return txKbps;
    }

    public String getRxRateText() {
        return rxRateText;
    }

    public String getTxRateText() {
        return txRateText;
    }
}







