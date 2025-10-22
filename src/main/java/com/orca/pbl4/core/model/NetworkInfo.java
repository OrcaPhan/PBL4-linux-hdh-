package com.orca.pbl4.core.model;

public class NetworkInfo {
    public final long rxBytes;
    public final long txBytes;

    public NetworkInfo(long rxBytes, long txBytes) {
        this.rxBytes = rxBytes;
        this.txBytes = txBytes;
    }
}
