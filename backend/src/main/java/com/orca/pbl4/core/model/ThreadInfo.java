package com.orca.pbl4.core.model;

public class ThreadInfo {
    private final int tid;
    private final String name;
    private final String state;
    private final long cpuTimeTicks;

    public ThreadInfo(int tid, String name, String state, long cpuTimeTicks) {
        this.tid = tid;
        this.name = name;
        this.state = state;
        this.cpuTimeTicks = cpuTimeTicks;

    }

    //getter
    public int getTid() {
        return tid;
    }
    public String getName() {
        return name;
    }
    public String getState() {
        return state;
    }
    public long getCpuTimeTicks() {
        return cpuTimeTicks;
    }


}
