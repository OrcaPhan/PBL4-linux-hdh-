package com.orca.pbl4.ui.metrics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.orca.pbl4.ui.metrics.MetricsPanel.DF1;
import static com.orca.pbl4.ui.metrics.MetricsPanel.MAX_POINTS;

/**
 * Panel vẽ đồ thị CPU per-core.
 * Giống GNOME System Monitor: mỗi core một đường màu riêng, không có đường tổng.
 */
public class CpuChartPanel extends JPanel {

    private final List<Deque<Float>> coreSeries = new ArrayList<>();
    private int coreCount = 0;



    static final Color[] CORE_COLORS = {
            new Color(0xFF0000),
            new Color(0xFB8C00),
            new Color(0xBA9502),
            new Color(0x72FF7A),
            new Color(0x00B13E),
            new Color(0x0CE3FF),
            new Color(0x0084FF),
            new Color(0x8E24AA),
            new Color(0xBC3FFF),
            new Color(0xFF00A3),
            new Color(0x4A0227),
            new Color(0x1A2F70),
    };


//    // Màu cho các core (mỗi core một màu, lặp lại nếu nhiều hơn số màu)
//    private static final Color[] CORE_COLORS = {
//        new Color(0x4ECDC4), // cyan
//        new Color(0x95E1D3), // light cyan
//        new Color(0xF38181), // light red
//        new Color(0xAA96DA), // purple
//        new Color(0xFCBAD3), // pink
//        new Color(0xFFFFD2), // yellow
//        new Color(0xC7CEEA), // light blue
//        new Color(0xFFB6C1), // light pink
//        new Color(0x98D8C8), // mint
//        new Color(0xFFCD00), // gold
//        new Color(0xBB8FCE), // lavender
//        new Color(0x85C1E2), // sky blue
//    };

    public CpuChartPanel() {
        setPreferredSize(new Dimension(0, 200));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("CPU"));
    }

    /**
     * Khởi tạo số lượng core và tạo Deque cho từng core.
     * @param cores Số lượng CPU cores
     */
    public void setCoreCount(int cores) {
        this.coreCount = Math.max(0, cores);
        // Đảm bảo có đủ Deque cho từng core
        while (coreSeries.size() < coreCount) {
            coreSeries.add(new ArrayDeque<>());
        }
        // Xóa các Deque thừa nếu coreCount giảm
        while (coreSeries.size() > coreCount) {
            coreSeries.remove(coreSeries.size() - 1);
        }
    }

    /**
     * Thêm một mẫu dữ liệu per-core.
     * @param perCorePercent Mảng %CPU của từng core (0-100), length = coreCount
     *                       Có thể chứa NaN nếu chưa có dữ liệu (sẽ được thay bằng 0f khi vẽ)
     */
    public void addPerCoreSample(float[] perCorePercent) {
        if (perCorePercent == null || coreSeries.isEmpty()) return;

        int actualCores = Math.min(Math.min(coreCount, perCorePercent.length), coreSeries.size());
        for (int i = 0; i < actualCores; i++) {
            Deque<Float> coreDeque = coreSeries.get(i);
            float value = perCorePercent[i];
            // Thay NaN bằng 0f để tránh lỗi khi vẽ (NaN sẽ hiển thị như 0%)
            if (Float.isNaN(value)) {
                value = 0f;
            }
            coreDeque.addLast(value);
            while (coreDeque.size() > MAX_POINTS) coreDeque.removeFirst();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (coreSeries.stream().allMatch(Deque::isEmpty)) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int chartY = 30;
        int chartH = h - chartY - 10;

        // Vẽ grid 0-100%
        g2.setColor(new Color(0xE0E0E0));
        for (int i = 0; i <= 5; i++) {
            int y = chartY + (i * chartH / 5);
            g2.drawLine(0, y, w, y);
            String label = (100 - i * 20) + "%";
            g2.setColor(Color.GRAY);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2.drawString(label, 5, y + 4);
            g2.setColor(new Color(0xE0E0E0));
        }

        // Vẽ các đường per-core (mỗi core một màu)
        for (int i = 0; i < coreCount && i < coreSeries.size(); i++) {
            Deque<Float> coreDeque = coreSeries.get(i);
            if (coreDeque.isEmpty()) continue;

            float[] series = ChartUtils.buildSeries(coreDeque);
            Path2D path = ChartUtils.createSmoothPathPercent(series, w, chartY, chartH);

            Color coreColor = CORE_COLORS[i % CORE_COLORS.length];
            g2.setColor(coreColor);
            g2.setStroke(new BasicStroke(1.75f)); // nét rõ nhưng không quá dày (1.5-2f)
            g2.draw(path);
        }

        g2.dispose();
    }
}
