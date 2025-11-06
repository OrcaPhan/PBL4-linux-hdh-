import com.orca.pbl4.core.model.*;
import com.orca.pbl4.core.system.reader.*;

import java.util.List;
import java.util.Locale;

public class procTest {
    public static void main(String[] args) {
        try {
            CpuStatReader cpuR = new CpuStatReader();
            MemInfoReader memR = new MemInfoReader();
            DiskStatReader diskR = new DiskStatReader();
            NetDevReader netR = new NetDevReader();
            ProcReader procR = new ProcReader();


            System.out.println("=== CPU ===");
            CpuInfo cpu = cpuR.read();
            System.out.printf(Locale.US, "user=%d nice=%d system=%d idle=%d iowait=%d irq=%d softirq=%d steal=%d guest=%d guestNice=%d cores=%d ",
                    cpu.getUser(), cpu.getNice(), cpu.getSystem(), cpu.getIdle(), cpu.getIowait(), cpu.getIrq(),
                    cpu.getSoftirq(), cpu.getSteal(), cpu.getGuest(), cpu.getGuestNice(), cpu.getCoreCount());

            System.out.println();
            System.out.println(" === Memory ===");
                    MemoryInfo mem = memR.read();

            System.out.printf(Locale.US, "MemTotal=%f KB MemUsed=%f KB Cached=%f KB SwapTotal=%d KB SwapUsed=%d KB ",
                    (float)mem.getTotalKB()/(1024*1024), (float)mem.getUsedKB()/(1024*1024),(float)mem.getCachedKB()/(1024*1024), mem.getSwapTotalKB(), mem.getSwapUsedKB());


            System.out.println();
            System.out.println(" === Disk (aggregate) ===");
                    DiskInfo d = diskR.readTotal();

            System.out.printf(Locale.US, "(bytes ~ read:%d write:%d) ",
                     d.readBytes(), d.writeBytes());


            System.out.println();
            System.out.println(" === Network (aggregate) ===");
                    NetworkInfo n = netR.readTotal();
            System.out.printf(Locale.US, "rxBytes=%d txBytes=%d ", n.getRxBytes(), n.getTxBytes());


            System.out.println();
            System.out.println(" === Processes (some) ===");
                            List<Integer> pids = procR.listPids();
            System.out.println("Total PIDs: " + pids.size());
            int shown = 0;
            for (int pid : pids) {
                ProcessInfo pi = procR.readOne(pid, false);
                if (pi == null) continue;

                System.out.println();
                System.out.printf(Locale.US, "pid=%d name=%s state=%s nice=%d cpuTicks=%d rssPages=%d ioR=%d ioW=%d cmd=%s ",
                        pi.getPid(), safe(pi.getName()), safe(pi.getState()), pi.getNice(),
                        pi.getProcCpuTicks(), pi.getRssPages(), pi.getIoReadBytes(), pi.getIoWriteBytes(),
                        shortCmd(pi.getCmdline()));
                if (++shown >= 20) break; // in 20 tiến trình đầu cho gọn
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("ProcQuickTest failed: " + t);
        }
    }


    private static String safe(String s) { return s == null ? "" : s; }


    private static String shortCmd(String s) {
        if (s == null) return "";
        s = s.replace(' ', ' ').trim();
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }
}
