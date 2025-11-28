package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.system.PasswdCache;

import java.util.*;

public class ProcReader {
    private final ProcFs proc;
    private final ThreadReader threadReader;
    private final HandleReader handleReader;

    public ProcReader() {
        this(new ProcFs());
    }

    public ProcReader(ProcFs proc) {
        this.proc = proc;
        this.threadReader = new ThreadReader(proc);
        this.handleReader = new HandleReader(proc);
    }

    public List<Integer> listPids() {
        return proc.listNumericDirs(""); // root /proc
    }

    public List<ProcessInfo> readAll(boolean withCmdline) {
        List<Integer> pids = listPids();
        List<ProcessInfo> out = new ArrayList<>(pids.size());
        for (int pid : pids) {
            ProcessInfo pi = readOne(pid, withCmdline);
            if (pi != null) out.add(pi);
        }
        return out;
    }

    public ProcessInfo readOne(int pid, boolean withCmdline) {
        try {
            String stat = proc.readString(String.valueOf(pid), "stat");
            String status = proc.readString(String.valueOf(pid), "status");
            String io = safeRead(pid, "io");
            String cmd = withCmdline ? safeRead(pid, "cmdline") : null;

            ProcessInfo p = new ProcessInfo();
            p.setPid(pid);

            ParsedStat s = parseStat(stat);
            p.setName(s.comm);
            p.setState(String.valueOf(s.state));
            p.setNice((int) s.nice);
            p.setProcCpuTicks(s.utime + s.stime);
            p.setStartTimeTicks(s.starttime);

// /proc/<pid>/status: tìm Uid, VmRSS
            Map<String, String> kv = parseStatus(status);
            String realUid  = kv.getOrDefault("NameUID", kv.getOrDefault("Uid", "?"));
            int Uid = Integer.parseInt(realUid.trim().split("\\s+")[0]);
            String user = PasswdCache.get().usernameOf(Uid);
            p.setUser(String.valueOf(user));


            long rssKB = parseVmRssKb(kv.get("VmRSS"));
            long pageSizeKB = 4;
            p.setRssPages(rssKB > 0 ? (rssKB / pageSizeKB) : 0);

            if (io != null) {
                Map<String, Long> ioMap = parseIo(io);
                p.setIoReadBytes(ioMap.getOrDefault("read_bytes", 0L));
                p.setIoWriteBytes(ioMap.getOrDefault("write_bytes", 0L));
            }

            if (withCmdline && cmd != null) {
                p.setCmdline(cmd.replace('\u0000', ' ').trim());
            }

            p.setThreads(Collections.<ThreadInfo>emptyList());

            return p;
        } catch (Exception e) {
            return null;
        }
    }

    public ProcessInfo readOneDetail(int pid) {
        ProcessInfo p = readOne(pid, true);
        if (p == null) return null;

        try {
            String statm = safeRead(pid, "statm");
            if (statm != null) {
                String[] parts = statm.trim().split("\\s+");
                if (parts.length >= 3) {
                    long sizePages = parseLong(parts, 0);      // total virtual memory
                    long sharePages = parseLong(parts, 2);     // shared memory
                    p.setVirtualPages(sizePages);
                    p.setSharedPages(sharePages);
                }
            }

            // Đọc threads
            List<ThreadInfo> threads = threadReader.readThreads(pid);
            p.setThreads(threads);

            // Đọc handles
            List<HandleInfo> handles = handleReader.readHandles(pid);
            p.setHandles(handles);

            String stat = safeRead(pid, "stat");
            if (stat != null) {
                ParsedStat s = parseStat(stat);
                String after = stat.substring(stat.lastIndexOf(')') + 1).trim();
                String[] rest = after.split("\\s+");
                long priority = parseLong(rest, 16 - 2); // 18th → index 16
                p.setPriority((int) priority);
            }

            return p;
        } catch (Exception e) {
            return p;
        }
    }
    private static class ParsedStat {
        String comm; char state; long utime; long stime; long nice; long starttime;
    }

    private static ParsedStat parseStat(String statContent) {
        int l = statContent.indexOf('(');
        int r = statContent.lastIndexOf(')');
        String comm = (l>=0 && r>l) ? statContent.substring(l+1, r) : "?";
        String before = statContent.substring(0, l).trim();
        String after = statContent.substring(r+1).trim();
        String[] rest = after.split("\\s+");
        ParsedStat ps = new ParsedStat();
        ps.comm = comm;
        ps.state = rest[0].charAt(0);
        ps.utime = parseLong(rest, 12 - 2); // 14th → index 12 sau khi bỏ (pid,comm)
        ps.stime = parseLong(rest, 13 - 2);
        ps.nice = parseLong(rest, 18 - 2);
        ps.starttime = parseLong(rest, 21 - 2);
        return ps;
    }

    private static long parseLong(String[] arr, int idx) {
        try { return (idx>=0 && idx < arr.length) ? Long.parseLong(arr[idx]) : 0L; } catch (Exception e) { return 0L; }
    }

    private static Map<String, String> parseStatus(String content) {
        Map<String, String> m = new HashMap<>();
        for (String line : content.split("\n")) {
            int c = line.indexOf(':');
            if (c < 0) continue;
            String k = line.substring(0, c).trim();
            String v = line.substring(c+1).trim();
            m.put(k, v);
        }
        return m;
    }

    private static long parseVmRssKb(String val) {
        if (val == null) return 0L;
        String[] p = val.trim().split("\\s+");
        try { return Long.parseLong(p[0]); } catch (Exception e) { return 0L; }
    }

    private String safeRead(int pid, String file) {
        try { return proc.readString(String.valueOf(pid), file); } catch (Exception e) { return null; }
    }

    private static Map<String, Long> parseIo(String content) {
        Map<String, Long> map = new HashMap<>();
        if (content == null || content.isEmpty()) return map;

        for (String line : content.split("\\n")) {
            int c = line.indexOf(':');
            if (c < 0) continue;
            String key = line.substring(0, c).trim();
            String valStr = line.substring(c + 1).trim().split("\\s+")[0];
            try {
                map.put(key, Long.parseLong(valStr));
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

}