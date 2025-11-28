package com.orca.pbl4.ui.metrics;

import com.orca.pbl4.core.model.CpuInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.system.SystemSnapshot;
import com.orca.pbl4.service.metrics.MetricsService;
import com.orca.pbl4.service.metrics.MetricsViewModel;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessUpdateListener;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

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

    // MetricsService để tính toán logic (tách khỏi UI)
    private final MetricsService metricsService = new MetricsService();

    // state CPU delta (tổng)
    private long prevCpuTotal = 0;
    private long prevCpuIdle = 0;
    // state CPU delta (per-core): lưu total ticks và idle ticks cho từng core
    private long[] prevPerCoreTotal = null;
    private long[] prevPerCoreIdle = null;

    // Lưu snapshot trước đó để tính delta cho MetricsService
    private SystemSnapshot previousSnapshot = null;

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

    public void attachTo(ProcessManager manager) {
        this.processManager = manager;
        manager.addUpdateListener(this);

        metricsTimer = new Timer(300, e -> {
            if (processManager != null && isVisible()) {
                processManager.refreshSnapshot();
            }
        });
        metricsTimer.setRepeats(true);
        metricsTimer.start();
    }

    @Override
    public synchronized void onProcessSnapshotUpdated(List<ProcessRow> rows, SystemSnapshot snapshot) {
        if (snapshot == null) return;

        SwingUtilities.invokeLater(() -> {
            CpuInfo cpu = snapshot.getCpu();

            //cpu
            // CPU calculation vẫn ở UI vì có logic phức tạp với per-core
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

            // ----- Memory & Network -----
            // Sử dụng MetricsService để tính toán (logic đã tách ra service)
            MetricsViewModel vm = metricsService.buildFromSnapshot(snapshot, previousSnapshot);
            
            // Cập nhật Memory từ ViewModel
            sidebar.updateMemory(vm.getMemPercentText(), snapshot.getMemory());
            memChart.addPoint(vm.getMemPercent());

            // Cập nhật Swap từ ViewModel
            sidebar.updateSwap(vm.isSwapAvailable() ? vm.getSwapText() : "not available");

            // Cập nhật Network từ ViewModel
            sidebar.updateNetwork(vm.getRxRateText(), vm.getTxRateText());
            netChart.addPoints(vm.getRxKbps(), vm.getTxKbps());

            // Lưu snapshot hiện tại để dùng cho lần sau
            previousSnapshot = snapshot;
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

}
