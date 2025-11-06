package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;

import java.nio.file.Path;
import java.util.*;

public class ProcReader {
    private final ProcFs proc;

    public ProcReader() { this(new ProcFs()); }
    public ProcReader(ProcFs proc) { this.proc = proc; }

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

// /proc/<pid>/stat: name trong ngoặc (), trạng thái ở cột 3, utime(14), stime(15), nice(19), starttime(22)
            ParsedStat s = parseStat(stat);
            p.setName(s.comm);
            p.setState(String.valueOf(s.state));
            p.setNice((int) s.nice);
            p.setProcCpuTicks(s.utime + s.stime);
            p.setStartTimeTicks(s.starttime);

// /proc/<pid>/status: tìm Uid, VmRSS
            Map<String, String> kv = parseStatus(status);
            p.setUser(kv.getOrDefault("NameUID", kv.getOrDefault("Uid", "?"))); // placeholder: service có thể map UID→user
            long rssKB = parseVmRssKb(kv.get("VmRSS"));
// model dùng rssPages → convert ngược theo 4KB/page (giả sử). Nếu bạn dùng rssKB trực tiếp, sửa model tuỳ ý.
            long pageSizeKB = 4;
            p.setRssPages(rssKB > 0 ? (rssKB / pageSizeKB) : 0);

// /proc/<pid>/io: rchar/wchar hoặc read_bytes/write_bytes
            if (io != null) {
                Map<String, Long> ioMap = parseIo(io);
                p.setIoReadBytes(ioMap.getOrDefault("read_bytes", 0L));
                p.setIoWriteBytes(ioMap.getOrDefault("write_bytes", 0L));
            }

            if (withCmdline && cmd != null) {
// cmdline ngăn cách bằng '\0'
                p.setCmdline(cmd.replace('\u0000', ' ').trim());
            }

// Threads: bỏ qua ở đây (để ThreadReader riêng hoặc mở rộng sau)
            p.setThreads(Collections.<ThreadInfo>emptyList());

// %CPU/%MEM để service set sau → để null
            return p;
        } catch (Exception e) {
            return null; // tiến trình có thể biến mất giữa chừng, trả null cho an toàn
        }
    }
    private static class ParsedStat {
        String comm; char state; long utime; long stime; long nice; long starttime;
    }

    private static ParsedStat parseStat(String statContent) {
// name trong ngoặc có thể chứa dấu cách → tách đặc biệt
        int l = statContent.indexOf('(');
        int r = statContent.lastIndexOf(')');
        String comm = (l>=0 && r>l) ? statContent.substring(l+1, r) : "?";
        String before = statContent.substring(0, l).trim();
        String after = statContent.substring(r+1).trim();
        String[] rest = after.split("\\s+");
// rest[0]=state, [11]=utime (14th overall), [12]=stime, [15]=starttime, [16]=vsize, [17]=rss ... nhưng vì chúng ta đã cắt nên dùng chỉ số tương đối
// Theo man proc: cột (bắt đầu từ 1): 3=state, 14=utime, 15=stime, 19=nice, 22=starttime
// Sau khi tách như trên, state = rest[0]
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