package com.orca.pbl4.core.model;

import java.util.*;

public class ProcessInfo {
    private final int pid;
    private final String name;
    private final String user;
    private final String state;
    private final int priority;
    private final int nice;

    private final long procCpuTicks; // utime +stime
    private final long rssPages; // số trang

    private final List<ThreadInfo> threads;
    private final List<HandleInfo> handles;
    private final String cmdline;
    private final long startTime;
    private final long ioReadBytes;
    private final long ioWriteBytes;

    private Float cpuPercent;
    private Float memoryPercent;

    private ProcessInfo(Builder b) {
    this.pid = b.pid;
    this.name = Objects.requireNonNull(b.name,"");
    this.user = Objects.requireNonNull(b.user,"");
    this.state = Objects.requireNonNull(b.state,"?");
    this.priority = b.priority;
    this.nice = b.nice;
    this.procCpuTicks = b.procCpuTicks;
    this.rssPages = b.rssPages;
    this.threads = new ArrayList<>(b.threads);
    this.handles = new ArrayList<>(b.handles);
    this.cmdline = b.cmdline;
    this.startTime = b.startTimeMs;
    this.ioReadBytes = b.ioReadBytes;
    this.ioWriteBytes = b.ioWriteBytes;
    this.cpuPercent = b.cpuPercent;
    this.memoryPercent = b.memPercent;

    }

    public static class Builder {
        private int pid;
        private String name;
        private String user;
        private String state;
        private int priority;
        private int nice;
        private long procCpuTicks;
        private long rssPages;
        private List<ThreadInfo> threads= new ArrayList<>();
        private List<HandleInfo> handles= new ArrayList<>();
        private String cmdline =  "";
        private long startTimeMs= 0L;
        private long ioReadBytes= 0L;
        private long ioWriteBytes= 0L;
        private Float cpuPercent= null;
        private Float memPercent= null;

        public Builder setPid(int p){ this.pid = p;  return this; }
        public Builder setName(String n){ this.name = n;  return this; }
        public Builder setUser(String u){ this.user = u;  return this; }
        public Builder setState(String s){ this.state = s;  return this; }
        public Builder setPriority(int p){ this.priority = p;  return this; }
        public Builder setNice(int n){ this.nice = n;  return this; }
        public Builder setProcCpuTicks(long p){ this.procCpuTicks = p;  return this; }
        public Builder setRssPages(long p){ this.rssPages = p;  return this; }
        public Builder setThreads(List<ThreadInfo> t){
            this.threads = (t==null?new ArrayList<>():t);
            return this; }
        public Builder setHandles(List<HandleInfo> h){
            this.handles = (h==null?new ArrayList<>():h);
            return this; }
        public Builder setCmdline(String c){
            this.cmdline = (c==null?"":c);
            return this; }
        public Builder setStartTime(long s){ this.startTimeMs = s; return this; }
        public Builder setIoReadBytes(long p){ this.ioReadBytes = p;  return this; }
        public Builder setIoWriteBytes(long p){ this.ioWriteBytes = p;  return this; }
        public Builder setCpuPer(Float c){ this.cpuPercent = c;  return this; }
        public Builder setMemoryPer(Float m){ this.memPercent = m;  return this; }

        public ProcessInfo build() {
            return new ProcessInfo(this);
        }
        public void addThread(ThreadInfo t){ this.threads.add(t);}
        public void addHandle(HandleInfo h){ this.handles.add(h); }

        //setter
        public void setCpuPercent(Float cpuPercent){
            this.cpuPercent = cpuPercent;
        }
        public  void setMemoryPercent(Float memPercent){
            this.memPercent = memPercent;
        }

        //getter
        public int getPid(){ return this.pid; }
        public String getName(){ return this.name; }
        public String getUser(){ return this.user; }
        public String getState(){ return this.state; }
        public int getPriority(){ return this.priority; }
        public int getNice(){ return this.nice; }
        public long getProcCpuTicks(){ return this.procCpuTicks; }
        public long getRssPages(){ return this.rssPages; }
        public List<ThreadInfo> getThreads(){ return Collections.unmodifiableList(this.threads); }
        public  List<HandleInfo> getHandles(){ return Collections.unmodifiableList(this.handles); }
        public String getCmdline(){ return this.cmdline; }
        public long getStartTime(){ return this.startTimeMs; }
        public long getIoReadBytes(){ return this.ioReadBytes; }
        public long getIoWriteBytes(){ return this.ioWriteBytes; }
        public Float getCpuPercent(){ return this.cpuPercent; }
        public Float getMemoryPercent(){ return this.memPercent; }

        public String toString(){
            return "ProcessInfo{pid=" + pid + ", name='" + name + "', user='" + user +
                    "', state='" + state + "', prio=" + priority + ", nice=" + nice +
                    ", cpuTicks=" + procCpuTicks + ", rssPages=" + rssPages +
                    ", cpu%=" + cpuPercent + ", mem%=" + memPercent + "}";
        }

    }
}
