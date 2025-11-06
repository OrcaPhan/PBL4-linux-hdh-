import com.orca.pbl4.core.model.*;
import com.orca.pbl4.core.system.*;

import java.util.*;

public class OrcaTopLike {
    private static final long SLEEP_MS = 1000; // refresh mỗi 1s
    private static final long PAGE_SIZE_KB = 4; // 4KB/page
    private static final int MAX_ROWS = 25;
    private static final boolean TOP_STYLE_PERCENT = false; // nhân theo coreCount

    public static void main(String[] args) throws Exception {
        SystemMonitor mon = new SystemMonitor();

        // Snapshot ban đầu
        SystemSnapshot prev = mon.getSystemSnapshot(false);

        while (true) {
            Thread.sleep(SLEEP_MS);

            SystemSnapshot now = mon.getSystemSnapshot(false);

            // xóa màn hình (Linux terminal)
            System.out.print("\033[H\033[2J");
            System.out.flush();

            printHeader();

            // ==== Tổng CPU/RAM ====
            double cpuTotalPct = cpuTotalPercent(prev.getCpu(), now.getCpu());
            System.out.printf(Locale.US,
                    "CPU tổng: %.1f%% | RAM: %s / %s\n",
                    cpuTotalPct,
                    humanKB(usedMemKB(now.getMemory())),
                    humanKB(now.getMemory().getTotalKB()));

            // ==== Tính delta process ====
            long deltaTotalCpu = now.getCpu().getTotalTicks() - prev.getCpu().getTotalTicks();
            int cores = Math.max(1, now.getCpu().getCoreCount());
            long memTotalKB = Math.max(1, now.getMemory().getTotalKB());

            // Map tick trước
            Map<Integer, Long> prevTicks = new HashMap<>();
            for (ProcessInfo p : prev.getProcesses()) {
                prevTicks.put(p.getPid(), safeLong(p.getProcCpuTicks()));
            }

            // Tính CPU% / MEM%
            List<Row> rows = new ArrayList<>();
            for (ProcessInfo p2 : now.getProcesses()) {
                long t2ticks = safeLong(p2.getProcCpuTicks());
                long t1ticks = prevTicks.getOrDefault(p2.getPid(), t2ticks);
                long dProc = Math.max(0, t2ticks - t1ticks);

                double cpuPct = (deltaTotalCpu <= 0) ? 0 :
                        100.0 * dProc / (double) deltaTotalCpu * (TOP_STYLE_PERCENT ? cores : 1);

                long rssKB = safeLong(p2.getRssPages()) * PAGE_SIZE_KB;
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

            // sắp xếp theo %CPU giảm dần
            rows.sort(Comparator.comparingDouble((Row r) -> r.cpuPct).reversed());

            printTableHeader();
            int count = 0;
            for (Row r : rows) {
                if (count++ >= MAX_ROWS) break;
                System.out.printf(Locale.US,
                        "%-6d %-10s %-28.28s %7.2f %7.2f %10s %-2s %5d\n",
                        r.pid, r.user, r.name, r.cpuPct, r.memPct, humanKB(r.rssKB), r.state, r.nice);
            }

            System.out.printf("\n(Ctrl + C để thoát, refresh mỗi %.1fs)\n", SLEEP_MS / 1000.0);

            // cập nhật snapshot trước
            prev = now;
            System.out.println("clear");
        }
    }

    // ==== Helpers ====
    private static void printHeader() {
        System.out.println("================ ORCA - Mini Top (live refresh) ================");
    }

    private static void printTableHeader() {
        System.out.printf("%-6s %-10s %-28s %7s %7s %10s %-2s %5s\n",
                "PID", "USER", "NAME", "%CPU", "%MEM", "RSS", "ST", "NICE");
        System.out.println("------ ---------- ---------------------------- ------- ------- ---------- -- -----");
    }

    private static double cpuTotalPercent(CpuInfo c1, CpuInfo c2) {
        long dTotal = c2.getTotalTicks() - c1.getTotalTicks();
        long dActive = (c2.getUser()-c1.getUser()) + (c2.getNice()-c1.getNice()) +
                (c2.getSystem()-c1.getSystem()) + (c2.getIrq()-c1.getIrq()) +
                (c2.getSoftirq()-c1.getSoftirq()) + (c2.getSteal()-c1.getSteal());
        if (dTotal <= 0) return 0.0;
        double base = 100.0 * ((double) dActive / (double) dTotal);
        int cores = Math.max(1, c2.getCoreCount());
        return TOP_STYLE_PERCENT ? base * cores : base;
    }

    private static long usedMemKB(MemoryInfo m) {
        long total = Math.max(0, m.getTotalKB());
        long avail = Math.max(0, m.getAvailableKB());
        return Math.max(0, total - avail);
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

    private record Row(int pid, String user, String name, double cpuPct,
                       double memPct, long rssKB, String state, int nice) {}
}
