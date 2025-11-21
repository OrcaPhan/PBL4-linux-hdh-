package com.orca.pbl4.ui.metrics;

import com.orca.pbl4.core.model.CpuInfo;
import com.orca.pbl4.core.model.MemoryInfo;
import com.orca.pbl4.core.model.NetworkInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessUpdateListener;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Tab "Metrics" – hiển thị sidebar + 3 đồ thị (CPU, Memory, Network).
 */
public class MetricsPanel extends JPanel implements ProcessUpdateListener {

    // dùng chung cho mọi chart
    public static final int MAX_POINTS = 60;        // 60 điểm trên 1 chart
    public static final DecimalFormat DF1 = new DecimalFormat("0.0");
    public static final DecimalFormat DF2 = new DecimalFormat("0.00");

    // sidebar + charts
    private final MetricsSidebarPanel sidebar = new MetricsSidebarPanel();
    private final CpuChartPanel cpuChart = new CpuChartPanel();
    private final MemoryChartPanel memChart = new MemoryChartPanel();
    private final NetworkChartPanel netChart = new NetworkChartPanel();

    // state CPU delta
    private long prevCpuTotal = 0;
    private long prevCpuIdle = 0;

    // state Net delta
    private long prevNetRx = 0;
    private long prevNetTx = 0;
    private long prevTimestamp = 0;

    public MetricsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        add(sidebar, BorderLayout.WEST);

        JPanel charts = new JPanel(new GridLayout(3, 1, 0, 8));
        charts.setBackground(Color.WHITE);
        charts.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        charts.add(cpuChart);
        charts.add(memChart);
        charts.add(netChart);

        add(charts, BorderLayout.CENTER);
    }

    // Cho ProcessManager đăng ký listener
    public void attachTo(ProcessManager manager) {
        manager.addUpdateListener(this);
    }

    @Override
    public synchronized void onProcessSnapshotUpdated(List<ProcessRow> rows, SystemSnapshot snapshot) {
        if (snapshot == null) return;

        SwingUtilities.invokeLater(() -> {
            CpuInfo cpu = snapshot.getCpu();
            MemoryInfo mem = snapshot.getMemory();
            NetworkInfo net = snapshot.getNetTotal();

            // ----- CPU -----
            float cpuPercent = computeCpuPercent(cpu);
            int cores = cpu.getCoreCount() > 0 ? cpu.getCoreCount()
                    : Runtime.getRuntime().availableProcessors();

            sidebar.updateCpu(DF1.format(cpuPercent) + "%", cores);
            cpuChart.addPoint(cpuPercent);

            // ----- Memory -----
            float memPercent = computeMemPercent(mem);
            sidebar.updateMemory(DF1.format(memPercent) + "%", mem);
            memChart.addPoint(memPercent);
//            sidebar.

            // swap
            long swapTotal = mem.getSwapTotalKB();
            if (swapTotal > 0) {
                long swapUsed = mem.getSwapUsedKB();
                float swapPercent = swapUsed * 100f / swapTotal;
                sidebar.updateSwap(DF1.format(swapPercent) + "%");
            } else {
                sidebar.updateSwap("not available");
            }

            // ----- Network -----
            float[] netRates = computeNetRates(net, snapshot.getCollectedAtNanos());
            float rxKbps = netRates[0];
            float txKbps = netRates[1];

            sidebar.updateNetwork(
                    formatBytesPerSec(rxKbps * 1024),
                    formatBytesPerSec(txKbps * 1024)
            );
            netChart.addPoints(rxKbps, txKbps);
        });
    }

    private float computeCpuPercent(CpuInfo cpu) {
        long total = cpu.getTotalTicks();
        long idle = cpu.getIdle() + cpu.getIowait();
        float percent = 0f;
        if (prevCpuTotal > 0) {
            long deltaTotal = total - prevCpuTotal;
            long deltaIdle = idle - prevCpuIdle;
            if (deltaTotal > 0) {
                percent = (deltaTotal - deltaIdle) * 100f / deltaTotal;
            }
        }
        prevCpuTotal = total;
        prevCpuIdle = idle;
        return Math.max(0f, Math.min(100f, percent));
    }

    private float computeMemPercent(MemoryInfo mem) {
        long total = mem.getTotalKB();
        long used = mem.getUsedKB();
        return total > 0
                ? Math.max(0f, Math.min(100f, used * 100f / total))
                : 0f;
    }

    private float[] computeNetRates(NetworkInfo net, long timestamp) {
        float rxKbps = 0f;
        float txKbps = 0f;
        if (net != null && prevTimestamp > 0 && timestamp > prevTimestamp) {
            long deltaRx = net.getRxBytes() - prevNetRx;
            long deltaTx = net.getTxBytes() - prevNetTx;
            double seconds = (timestamp - prevTimestamp) / 1_000_000_000d;
            if (seconds > 0) {
                rxKbps = (float) ((deltaRx / 1024d) / seconds);
                txKbps = (float) ((deltaTx / 1024d) / seconds);
            }
        }
        if (net != null) {
            prevNetRx = net.getRxBytes();
            prevNetTx = net.getTxBytes();
        }
        prevTimestamp = timestamp;
        return new float[]{Math.max(0f, rxKbps), Math.max(0f, txKbps)};
    }

    private String formatBytesPerSec(float bytesPerSec) {
        if (bytesPerSec < 1024) return DF1.format(bytesPerSec) + " bytes/s";
        if (bytesPerSec < 1024 * 1024) return DF1.format(bytesPerSec / 1024f) + " KB/s";
        return DF2.format(bytesPerSec / (1024f * 1024f)) + " MB/s";
    }
}
