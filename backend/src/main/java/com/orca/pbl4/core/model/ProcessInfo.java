package com.orca.pbl4.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Thông tin một tiến trình (process) đọc từ /proc/<pid>/...
 * Dạng Java Bean: có getter + setter.
 */
public class ProcessInfo {

    private int pid;
    private String name;
    private String user;
    private String state;
    private int priority;
    private int nice;

    private long procCpuTicks;
    private long rssPages;
    private long startTimeTicks;

    private long ioReadBytes;
    private long ioWriteBytes;
    private String cmdline;

    private List<ThreadInfo> threads = new ArrayList<>();
    private List<HandleInfo> handles = new ArrayList<>();

    private Float cpuPercent;
    private Float memoryPercent;
    private long collectedAtNanos;

    // ── Constructors
    public ProcessInfo() {
        this.collectedAtNanos = System.nanoTime();
    }

    public ProcessInfo(int pid, String name, String user) {
        this.pid = pid;
        this.name = name;
        this.user = user;
        this.collectedAtNanos = System.nanoTime();
    }

    // ── Getter/Setter
    public int getPid() { return pid; }
    public void setPid(int pid) { this.pid = pid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getNice() { return nice; }
    public void setNice(int nice) { this.nice = nice; }

    public long getProcCpuTicks() { return procCpuTicks; }
    public void setProcCpuTicks(long procCpuTicks) { this.procCpuTicks = procCpuTicks; }

    public long getRssPages() { return rssPages; }
    public void setRssPages(long rssPages) { this.rssPages = rssPages; }

    public long getStartTimeTicks() { return startTimeTicks; }
    public void setStartTimeTicks(long startTimeTicks) { this.startTimeTicks = startTimeTicks; }

    public long getIoReadBytes() { return ioReadBytes; }
    public void setIoReadBytes(long ioReadBytes) { this.ioReadBytes = ioReadBytes; }

    public long getIoWriteBytes() { return ioWriteBytes; }
    public void setIoWriteBytes(long ioWriteBytes) { this.ioWriteBytes = ioWriteBytes; }

    public String getCmdline() { return cmdline; }
    public void setCmdline(String cmdline) { this.cmdline = cmdline; }

    public List<ThreadInfo> getThreads() { return threads; }
    public void setThreads(List<ThreadInfo> threads) { this.threads = threads; }

    public List<HandleInfo> getHandles() { return handles; }
    public void setHandles(List<HandleInfo> handles) { this.handles = handles; }

    public Float getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(Float cpuPercent) { this.cpuPercent = cpuPercent; }

    public Float getMemoryPercent() { return memoryPercent; }
    public void setMemoryPercent(Float memoryPercent) { this.memoryPercent = memoryPercent; }

    public long getCollectedAtNanos() { return collectedAtNanos; }
    public void setCollectedAtNanos(long collectedAtNanos) { this.collectedAtNanos = collectedAtNanos; }


}
