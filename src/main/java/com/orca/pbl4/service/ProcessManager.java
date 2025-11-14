package com.orca.pbl4.service;

import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.util.*;

/**
 * ProcessManager (phiên bản mượt & ổn định hơn)
 * - Giữ snapshot cũ để tính delta nhanh hơn
 * - Làm mượt %CPU bằng trung bình trượt (EMA)
 * - Tự fix %MEM nếu MemTotal đang tính bằng byte
 * - Search / sort vẫn cập nhật liên tục
 */
public class ProcessManager {
    private final SystemMonitor monitor;
    private final int period; // ms
    private SystemSnapshot oldSnap;
    private final Map<Integer, Float> smoothCpu = new HashMap<>();
    private static final float ALPHA = 0.3f; // hệ số EMA

    public ProcessManager(SystemMonitor monitor, int period) {
        this.monitor = monitor;
        this.period = period;
        this.oldSnap = null;
    }

    public ProcessManager(SystemMonitor monitor) {
        this(monitor, 1000);
    }

    /** Lấy danh sách ProcessRow mượt */
    public List<ProcessRow> listProcessRows() {
        SystemSnapshot prevSnap = oldSnap;
        SystemSnapshot currSnap;

        // Nếu lần đầu tiên gọi, đọc 2 lần để có delta
        if (prevSnap == null) {
            prevSnap = monitor.getSystemSnapshot(false);
            sleepQuiet(period);
            currSnap = monitor.getSystemSnapshot(false);
        } else {
            // Nếu đã có snapshot trước thì chờ đủ period rồi chụp snapshot mới
            sleepQuiet(period);
            currSnap = monitor.getSystemSnapshot(false);
        }

        oldSnap = currSnap;

        // delta CPU toàn hệ
        long deltaTotal = Math.max(1,
                currSnap.getCpu().getTotalTicks() - prevSnap.getCpu().getTotalTicks());

        // RAM total (fix nếu đơn vị là byte)
        long totalMemKB = currSnap.getMemory().getTotalKB();
        if (totalMemKB > 1_000_000_000L) totalMemKB /= 1024; // nếu đang là bytes

        long pageKB = 4; // mặc định Linux
        int cores = currSnap.getCpu().getCoreCount() > 0
                ? currSnap.getCpu().getCoreCount()
                : Runtime.getRuntime().availableProcessors();

        // Map PID -> ProcessInfo cũ
        Map<Integer, ProcessInfo> oldMap = new HashMap<>();
        for (ProcessInfo p : prevSnap.getProcesses()) oldMap.put(p.getPid(), p);

        List<ProcessRow> rows = new ArrayList<>();

        for (ProcessInfo cur : currSnap.getProcesses()) {
            int pid = cur.getPid();
            ProcessInfo old = oldMap.get(pid);

            // delta ticks CPU tiến trình
            long deltaProc = 0;
            if (old != null)
                deltaProc = Math.max(0, cur.getProcCpuTicks() - old.getProcCpuTicks());

            // Tính %CPU
            float cpuPercent = deltaProc * 100f / deltaTotal;
            cpuPercent *= cores; // giống 'top' (tính theo số core)

            // Làm mượt %CPU bằng EMA
            float prev = smoothCpu.getOrDefault(pid, cpuPercent);
            float smooth = (1 - ALPHA) * prev + ALPHA * cpuPercent;
            smoothCpu.put(pid, smooth);

            // %MEM
            long rssKB = cur.getRssPages() * pageKB;
            float memPercent = (totalMemKB > 0) ? rssKB * 100f / totalMemKB : 0;

            // format
            String memoryStr = formatKB(rssKB);
            String diskReadStr = cur.getIoReadBytes() > 0 ? formatKB(cur.getIoReadBytes() / 1024) : "-";
            String diskWriteStr = cur.getIoWriteBytes() > 0 ? formatKB(cur.getIoWriteBytes() / 1024) : "-";

            // priority
            int nice = cur.getNice();
            String level;
            if (nice <= -10) level = "High";
            else if (nice < 0) level = "Above Normal";
            else if (nice == 0) level = "Normal";
            else if (nice <= 10) level = "Below Normal";
            else level = "Low";

            // tạo ProcessRow
            ProcessRow r = new ProcessRow();
            r.setPid(pid);
            r.setName(safe(cur.getName(), "?"));
            r.setUser(safe(cur.getUser(), "?"));
            r.setCpuPercent(smooth);
            r.setMemoryStr(memoryStr);
            r.setMemPercent(memPercent);
            r.setDiskRead(diskReadStr);
            r.setDiskWrite(diskWriteStr);
            r.setPriority(level);
            r.setState(cur.getState() != null && !cur.getState().isEmpty()
                    ? cur.getState().charAt(0) : '?');

            rows.add(r);
        }

        // sort theo CPU giảm dần
        rows.sort((a, b) -> Float.compare(b.getCpuPercent(), a.getCpuPercent()));
        return rows;
    }

    /* ---------------- SEARCH & SORT ---------------- */

    public List<ProcessRow> search(String keyword) {
        List<ProcessRow> all = listProcessRows();
        if (keyword == null || keyword.isBlank()) return all;

        String key = keyword.toLowerCase(Locale.ROOT);
        List<ProcessRow> out = new ArrayList<>();

        try {
            int pid = Integer.parseInt(key);
            for (ProcessRow r : all) if (r.getPid() == pid) out.add(r);
            if (!out.isEmpty()) return out;
        } catch (NumberFormatException ignored) {}

        for (ProcessRow r : all) {
            if (r.getName().toLowerCase().contains(key)
                    || r.getUser().toLowerCase().contains(key)) {
                out.add(r);
            }
        }
        return out;
    }

    public List<ProcessRow> sort(String column, boolean descending) {
        List<ProcessRow> list = listProcessRows();
        String c = (column == null) ? "cpu" : column.toLowerCase(Locale.ROOT);

        Comparator<ProcessRow> cmp;
        switch (c) {
            case "pid":
                cmp = Comparator.comparingInt(ProcessRow::getPid);
                break;
            case "name":
                cmp = Comparator.comparing(ProcessRow::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "user":
                cmp = Comparator.comparing(ProcessRow::getUser, String.CASE_INSENSITIVE_ORDER);
                break;
            case "mem":
                cmp = Comparator.comparingDouble(ProcessRow::getMemPercent);
                break;
            case "priority":
                cmp = Comparator.comparing(ProcessRow::getPriority, String.CASE_INSENSITIVE_ORDER);
                break;
            default:
                cmp = Comparator.comparingDouble(ProcessRow::getCpuPercent);
                break;
        }
        if (descending) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    /* ---------------- UTILS ---------------- */

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static String formatKB(long kb) {
        if (kb < 1024) return kb + " KB";
        float mb = kb / 1024f;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.1f GB", mb / 1024f);
    }
}
