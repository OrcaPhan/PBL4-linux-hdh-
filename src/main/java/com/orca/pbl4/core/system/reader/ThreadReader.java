package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.ThreadInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đọc thông tin threads từ /proc/<pid>/task.
 * Mỗi thread có thư mục /proc/<pid>/task/<tid>/ với file stat tương tự /proc/<pid>/stat.
 */
public class ThreadReader {
    private final ProcFs proc;

    public ThreadReader() {
        this(new ProcFs());
    }

    public ThreadReader(ProcFs proc) {
        this.proc = proc;
    }

    /**
     * Đọc danh sách threads của một process.
     * @param pid Process ID
     * @return Danh sách ThreadInfo, mỗi phần tử là một thread
     */
    public List<ThreadInfo> readThreads(int pid) {
        List<Integer> tids;
        try {
            tids = proc.listNumericDirs(String.valueOf(pid), "task");
        } catch (Exception e) {
            return Collections.emptyList();
        }

        List<ThreadInfo> threads = new ArrayList<>();
        for (int tid : tids) {
            ThreadInfo thread = readThread(pid, tid);
            if (thread != null) {
                threads.add(thread);
            }
        }
        return threads;
    }

    /**
     * Đọc thông tin một thread cụ thể.
     * @param pid Process ID
     * @param tid Thread ID
     * @return ThreadInfo hoặc null nếu không đọc được
     */
    private ThreadInfo readThread(int pid, int tid) {
        try {
            String stat = proc.readString(String.valueOf(pid), "task", String.valueOf(tid), "stat");
            ParsedThreadStat parsed = parseThreadStat(stat, tid);
            return new ThreadInfo(tid, parsed.name, String.valueOf(parsed.state), parsed.cpuTicks);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse /proc/<pid>/task/<tid>/stat tương tự parseStat trong ProcReader.
     * Format: pid (comm) state ... utime(14) stime(15) ...
     */
    private static ParsedThreadStat parseThreadStat(String statContent, int tid) {
        int l = statContent.indexOf('(');
        int r = statContent.lastIndexOf(')');
        String name = (l >= 0 && r > l) ? statContent.substring(l + 1, r) : "?";
        String after = statContent.substring(r + 1).trim();
        String[] rest = after.split("\\s+");

        // state ở rest[0], utime ở rest[12] (14th overall), stime ở rest[13] (15th overall)
        char state = rest.length > 0 ? rest[0].charAt(0) : '?';
        long utime = parseLong(rest, 12 - 2); // 14th → index 12 sau khi bỏ (pid,comm)
        long stime = parseLong(rest, 13 - 2);  // 15th → index 13
        long cpuTicks = utime + stime;

        ParsedThreadStat ps = new ParsedThreadStat();
        ps.name = name;
        ps.state = state;
        ps.cpuTicks = cpuTicks;
        return ps;
    }

    private static long parseLong(String[] arr, int idx) {
        try {
            return (idx >= 0 && idx < arr.length) ? Long.parseLong(arr[idx]) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static class ParsedThreadStat {
        String name;
        char state;
        long cpuTicks;
    }
}
