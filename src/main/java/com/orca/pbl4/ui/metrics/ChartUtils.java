package com.orca.pbl4.ui.metrics;

import java.awt.geom.Path2D;
import java.util.Deque;

import static com.orca.pbl4.ui.metrics.MetricsPanel.MAX_POINTS;

public class ChartUtils {

    // dồn điểm thật về bên phải, phần đầu fill bằng giá trị đầu tiên
    public static float[] buildSeries(Deque<Float> src) {
        float[] arr = new float[MAX_POINTS];
        if (src.isEmpty()) return arr;

        float first = src.peekFirst();
        for (int i = 0; i < MAX_POINTS; i++) arr[i] = first;

        int n = src.size();
        int idx = Math.max(0, MAX_POINTS - n);
        for (Float v : src) {
            if (idx >= 0 && idx < MAX_POINTS) arr[idx] = v;
            idx++;
        }
        return arr;
    }

    // series là % (0-100)
    public static Path2D createSmoothPathPercent(float[] series, int w, int chartY, int chartH) {
        return createSmoothPath(series, w, chartY, chartH, 100f);
    }

    // series là rate (0 - maxVal)
    public static Path2D createSmoothPathRate(float[] series, int w, int chartY, int chartH, float maxVal) {
        return createSmoothPath(series, w, chartY, chartH, maxVal);
    }

    private static Path2D createSmoothPath(float[] series, int w, int chartY, int chartH, float maxVal) {
        int n = series.length;
        Path2D path = new Path2D.Float();
        if (n == 0) return path;

        float stepX = n > 1 ? (float) (w - 20) / (n - 1) : 0f;

        float[] xs = new float[n];
        float[] ys = new float[n];

        for (int i = 0; i < n; i++) {
            xs[i] = 10 + i * stepX;
            float ratio = maxVal <= 0 ? 0f : Math.min(1f, series[i] / maxVal);
            ys[i] = chartY + chartH - ratio * chartH;
        }

        path.moveTo(xs[0], ys[0]);

        if (n < 4) {
            for (int i = 1; i < n; i++) path.lineTo(xs[i], ys[i]);
            return path;
        }

        // Catmull-Rom ~ Bezier
        for (int i = 0; i < n - 1; i++) {
            int i0 = Math.max(i - 1, 0);
            int i1 = i;
            int i2 = i + 1;
            int i3 = Math.min(i + 2, n - 1);

            float x0 = xs[i0], y0 = ys[i0];
            float x1 = xs[i1], y1 = ys[i1];
            float x2 = xs[i2], y2 = ys[i2];
            float x3 = xs[i3], y3 = ys[i3];

            float cx1 = x1 + (x2 - x0) / 6f;
            float cy1 = y1 + (y2 - y0) / 6f;
            float cx2 = x2 - (x3 - x1) / 6f;
            float cy2 = y2 - (y3 - y1) / 6f;

            path.curveTo(cx1, cy1, cx2, cy2, x2, y2);
        }

        return path;
    }
}
