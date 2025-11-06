package com.orca.pbl4.core.model;

public class NetworkInfo {
    private final long rxBytes;
    private final long txBytes;

    public NetworkInfo(long rxBytes, long txBytes) {
        this.rxBytes = rxBytes;
        this.txBytes = txBytes;
    }
    public long getRxBytes() {
        return rxBytes;
    }
    public long getTxBytes() {
        return txBytes;
    }
}
