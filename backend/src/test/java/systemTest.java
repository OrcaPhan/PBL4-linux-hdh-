import com.orca.pbl4.core.model.CpuInfo;
import com.orca.pbl4.core.model.MemoryInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.system.SystemMonitor;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.util.*;

public class systemTest {
    private static final long SLEEP_MS = 1000; // cửa sổ đo ~1s
    private static final long PAGE_SIZE_KB = 4; // giả định 4KB/page (phổ biến)
    private static final int MAX_ROWS = 30; // in tối đa 30 dòng cho gọn
    private static final boolean TOP_STYLE_PERCENT = true; // true: nhân coreCount như 'top'

    public static void main(String[] args) throws Exception {
        SystemMonitor mon = new SystemMonitor();
        try {
//            while (true) {
//                SystemSnapshot s1 = mon.getSystemSnapshot(false);
//            }
// Snapshot 1
            SystemSnapshot s1 = mon.getSystemSnapshot(false); // không cần cmdline, cho nhanh
// đợi ~1s
            Thread.sleep(SLEEP_MS);
// Snapshot 2
            SystemSnapshot s2 = mon.getSystemSnapshot(false);


            printHeader();


            double cpuTotalPct = cpuTotalPercent(s1.getCpu(), s2.getCpu());
            System.out.println();
            System.out.printf(Locale.US, "CPU tổng: %.1f%% | RAM: %s / %s ",
                    cpuTotalPct,
                    humanKB(usedMemKB(s2.getMemory())),
                    humanKB(s2.getMemory().getTotalKB()));
            System.out.println();

// Build map pid->ticks ở t1
            Map<Integer, Long> prevTicks = new HashMap<>();
            for (ProcessInfo p : s1.getProcesses()) {
                prevTicks.put(p.getPid(), safeLong(p.getProcCpuTicks()));
            }


// Tính %CPU/%MEM cho snapshot t2
            List<Row> rows = new ArrayList<>();
            long deltaTotalCpu = s2.getCpu().getTotalTicks() - s1.getCpu().getTotalTicks();
            int cores = Math.max(1, s2.getCpu().getCoreCount());
            long memTotalKB = Math.max(1, s2.getMemory().getTotalKB());


            for (ProcessInfo p2 : s2.getProcesses()) {
                long t2ticks = safeLong(p2.getProcCpuTicks());
                long t1ticks = prevTicks.getOrDefault(p2.getPid(), t2ticks); // nếu mới xuất hiện
                long dProc = Math.max(0, t2ticks - t1ticks);
                double cpuPct;
                if (deltaTotalCpu <= 0) cpuPct = 0;
                else {
                    double base = 100.0 * ((double) dProc / (double) deltaTotalCpu);
                    cpuPct = TOP_STYLE_PERCENT ? base : 1.0; // top-style nhân coreCount
                }


                long rssPages = safeLong(p2.getRssPages());
                long rssKB = rssPages * PAGE_SIZE_KB;
                double memPct = 100.0 * ((double) rssKB / (double) memTotalKB);


                rows.add(new Row(
                        p2.getPid(),
                        nz(p2.getUser()),
                        nz(p2.getName()),
                        cpuPct,
                        memPct,
                        rssKB,
                        nz(p2.getState()),
                        p2.getNice()
                ));
            }
            // Sắp xếp theo %CPU giảm dần
            rows.sort(Comparator.comparingDouble((Row r) -> r.cpuPct).reversed());


// In bảng
            printTableHeader();
            int count = 0;
            for (Row r : rows) {
                if (count++ >= MAX_ROWS) break;
                System.out.println();
                System.out.printf(Locale.US,
                        "%-6d %-10s %-28.28s %7.2f %7.2f %10s %-2s %5d ",
                        r.pid, r.user, r.name, r.cpuPct, r.memPct, humanKB(r.rssKB), r.state, r.nice);
            }

            System.out.println();
            System.out.println(" Gợi ý: Thử thay SLEEP_MS=2000 để cửa sổ đo rộng hơn, số liệu mượt hơn.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // ==== Helpers ====
    private static void printHeader() {
        System.out.println();
        System.out.println("================ ORCA TopLikeTest (raw system + delta 1s) ================");
    }


    private static void printTableHeader() {
        System.out.println();
        System.out.printf("%-6s %-10s %-28s %7s %7s %10s %-2s %5s ",
                "PID", "USER", "NAME", "%CPU", "%MEM", "RSS", "ST", "NICE");
        System.out.println("------ ---------- ---------------------------- ------- ------- ---------- -- -----");
    }


    private static double cpuTotalPercent(CpuInfo c1, CpuInfo c2) {
        long dTotal = c2.getTotalTicks() - c1.getTotalTicks();
        long dActive = (c2.getUser()-c1.getUser()) + (c2.getNice()-c1.getNice()) + (c2.getSystem()-c1.getSystem())
                + (c2.getIrq()-c1.getIrq()) + (c2.getSoftirq()-c1.getSoftirq()) + (c2.getSteal()-c1.getSteal());
        if (dTotal <= 0) return 0.0;
        double base = 100.0 * ((double) dActive / (double) dTotal);
        int cores = Math.max(1, c2.getCoreCount());
        return TOP_STYLE_PERCENT ? base : 1.0;
    }


    private static long usedMemKB(MemoryInfo m) {
        long total = Math.max(0, m.getTotalKB());
        long avail = Math.max(0, m.getAvailableKB());
        long used = total - avail; // gần giống GNOME System Monitor
        return Math.max(0, used);
    }


    private static String humanKB(long kb) {
        double v = kb;
        String[] units = {"KB","MB","GB","TB"};
        int u = 0;
        while (v >= 1024 && u < units.length-1) { v /= 1024; u++; }
        return String.format(Locale.US, "%.1f %s", v, units[u]);
    }


    private static long safeLong(Long x) { return x == null ? 0L : x; }
    private static String nz(String s) { return s == null ? "" : s; }


    private record Row(int pid, String user, String name, double cpuPct, double memPct,
                       long rssKB, String state, int nice) {}
}

