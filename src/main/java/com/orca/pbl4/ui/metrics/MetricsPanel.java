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
    public static final int MAX_POINTS = 100;        // 100 điểm trên 1 chart (tăng từ 60 để đường mượt hơn)
    public static final DecimalFormat DF1 = new DecimalFormat("0.0");
    public static final DecimalFormat DF2 = new DecimalFormat("0.00");

    // sidebar + charts
    private final MetricsSidebarPanel sidebar = new MetricsSidebarPanel();
    private final CpuChartPanel cpuChart = new CpuChartPanel();
    private final MemoryChartPanel memChart = new MemoryChartPanel();
    private final NetworkChartPanel netChart = new NetworkChartPanel();

    // state CPU delta (tổng)
    private long prevCpuTotal = 0;
    private long prevCpuIdle = 0;
    // state CPU delta (per-core): lưu total ticks và idle ticks cho từng core
    private long[] prevPerCoreTotal = null;
    private long[] prevPerCoreIdle = null;

    // state Net delta
    private long prevNetRx = 0;
    private long prevNetTx = 0;
    private long prevTimestamp = 0;

    // Timer riêng cho Metrics để sampling nhanh hơn (250-300ms thay vì 1s)
    private Timer metricsTimer;
    private ProcessManager processManager;

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

    // Cho ProcessManager đăng ký listener và khởi động timer riêng cho Metrics
    public void attachTo(ProcessManager manager) {
        this.processManager = manager;
        manager.addUpdateListener(this);
        
        // Tạo timer riêng cho Metrics với period 300ms để đường cong mượt hơn
        // Timer này chỉ refresh snapshot cho Metrics, không ảnh hưởng Process list (vẫn 1s)
        metricsTimer = new Timer(300, e -> {
            if (processManager != null && isVisible()) {
                // refreshSnapshot() sẽ gọi notifyListeners() → onProcessSnapshotUpdated()
                // Tất cả đều chạy trên EDT hoặc background thread, không block UI
                processManager.refreshSnapshot();
            }
        });
        metricsTimer.setRepeats(true);
        metricsTimer.start();
    }

    /**
     * Dừng timer khi panel không còn được sử dụng (optional, để tối ưu).
     */
    public void stopTimer() {
        if (metricsTimer != null) {
            metricsTimer.stop();
        }
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

            // Tính %CPU per-core
            float[] perCorePercent = computePerCoreCpuPercent(snapshot.getPerCoreCpus());
            
            // Cập nhật sidebar với legend per-core
            sidebar.updateCpuCores(cpuPercent, perCorePercent);
            
            // Cập nhật chart per-core
            cpuChart.setCoreCount(cores);
            cpuChart.addPerCoreSample(perCorePercent);

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

    /**
     * Tính %CPU tổng từ delta ticks.
     * @param cpu CpuInfo tổng từ snapshot
     * @return %CPU (0-100)
     */
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

    /**
     * Tính %CPU cho từng core từ delta ticks.
     * @param perCoreCpus Danh sách CpuInfo per-core từ snapshot
     * @return Mảng float[] chứa %CPU của từng core (0-100), length = số core
     */
    private float[] computePerCoreCpuPercent(List<CpuInfo> perCoreCpus) {
        if (perCoreCpus == null || perCoreCpus.isEmpty()) {
            return new float[0];
        }

        int coreCount = perCoreCpus.size();
        float[] perCorePercent = new float[coreCount];

        // Khởi tạo mảng prev nếu chưa có hoặc size khác
        if (prevPerCoreTotal == null || prevPerCoreTotal.length != coreCount) {
            prevPerCoreTotal = new long[coreCount];
            prevPerCoreIdle = new long[coreCount];
            // Lần đầu chưa có delta, trả về NaN để hiển thị "--.-%"
            for (int i = 0; i < coreCount; i++) {
                CpuInfo core = perCoreCpus.get(i);
                prevPerCoreTotal[i] = core.getTotalTicks();
                prevPerCoreIdle[i] = core.getIdle() + core.getIowait();
                perCorePercent[i] = Float.NaN; // Chưa có dữ liệu
            }
            return perCorePercent;
        }

        // Tính delta cho từng core
        for (int i = 0; i < coreCount; i++) {
            CpuInfo core = perCoreCpus.get(i);
            long total = core.getTotalTicks();
            long idle = core.getIdle() + core.getIowait();

            float percent = 0f;
            if (prevPerCoreTotal[i] > 0) {
                long deltaTotal = total - prevPerCoreTotal[i];
                long deltaIdle = idle - prevPerCoreIdle[i];
                if (deltaTotal > 0) {
                    percent = (deltaTotal - deltaIdle) * 100f / deltaTotal;
                }
            }

            prevPerCoreTotal[i] = total;
            prevPerCoreIdle[i] = idle;
            perCorePercent[i] = Math.max(0f, Math.min(100f, percent));
        }

        return perCorePercent;
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
