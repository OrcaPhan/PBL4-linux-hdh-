package com.orca.pbl4.service.metrics;

import com.orca.pbl4.core.model.MemoryInfo;
import com.orca.pbl4.core.model.NetworkInfo;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.text.DecimalFormat;

public class MetricsService {

    private static final DecimalFormat DF1 = new DecimalFormat("0.0");
    private static final DecimalFormat DF2 = new DecimalFormat("0.00");

    // State để tính delta cho network
    private long prevNetRx = 0;
    private long prevNetTx = 0;
    private long prevTimestamp = 0;

    public MetricsViewModel buildFromSnapshot(SystemSnapshot snapshot, SystemSnapshot previousSnapshot) {
        if (snapshot == null) {
            return createEmptyViewModel();
        }

        MemoryInfo mem = snapshot.getMemory();
        NetworkInfo net = snapshot.getNetTotal();
        long timestamp = snapshot.getCollectedAtNanos();

        // Tính %MEM
        float memPercent = computeMemPercent(mem);
        String memPercentText = DF1.format(memPercent) + "%";

        // Tính %SWAP
        boolean swapAvailable = mem.getSwapTotalKB() > 0;
        float swapPercent = 0f;
        String swapText;
        if (swapAvailable) {
            swapPercent = computeSwapPercent(mem);
            swapText = DF1.format(swapPercent) + "%";
        } else {
            swapText = "not available";
        }

        // Tính network rates (rxKbps, txKbps)
        float[] netRates = computeNetRates(net, timestamp, previousSnapshot);
        float rxKbps = netRates[0];
        float txKbps = netRates[1];

        // Format network rates
        String rxRateText = formatBytesPerSec(rxKbps * 1024);
        String txRateText = formatBytesPerSec(txKbps * 1024);

        return new MetricsViewModel(
            memPercent, memPercentText,
            swapAvailable, swapPercent, swapText,
            rxKbps, txKbps, rxRateText, txRateText
        );
    }

    private float computeMemPercent(MemoryInfo mem) {
        long total = mem.getTotalKB();
        long used = mem.getUsedKB();
        return total > 0
                ? Math.max(0f, Math.min(100f, used * 100f / total))
                : 0f;
    }

    private float computeSwapPercent(MemoryInfo mem) {
        long swapTotal = mem.getSwapTotalKB();
        if (swapTotal <= 0) return 0f;
        long swapUsed = mem.getSwapUsedKB();
        return Math.max(0f, Math.min(100f, swapUsed * 100f / swapTotal));
    }

    private float[] computeNetRates(NetworkInfo net, long timestamp, SystemSnapshot previousSnapshot) {
        float rxKbps = 0f;
        float txKbps = 0f;

        if (net == null) {
            return new float[]{0f, 0f};
        }

        long currentRx = net.getRxBytes();
        long currentTx = net.getTxBytes();

        // Nếu có previousSnapshot, dùng nó để tính delta (không cập nhật state nội bộ)
        if (previousSnapshot != null && previousSnapshot.getNetTotal() != null) {
            NetworkInfo prevNet = previousSnapshot.getNetTotal();
            long prevTimestamp = previousSnapshot.getCollectedAtNanos();
            
            if (prevTimestamp > 0 && timestamp > prevTimestamp) {
                long deltaRx = currentRx - prevNet.getRxBytes();
                long deltaTx = currentTx - prevNet.getTxBytes();
                double seconds = (timestamp - prevTimestamp) / 1_000_000_000d;
                if (seconds > 0) {
                    rxKbps = (float) ((deltaRx / 1024d) / seconds);
                    txKbps = (float) ((deltaTx / 1024d) / seconds);
                }
            }
        } else {
            // Dùng state nội bộ để tính delta (khi không có previousSnapshot)
            if (prevTimestamp > 0 && timestamp > prevTimestamp) {
                long deltaRx = currentRx - prevNetRx;
                long deltaTx = currentTx - prevNetTx;
                double seconds = (timestamp - prevTimestamp) / 1_000_000_000d;
                if (seconds > 0) {
                    rxKbps = (float) ((deltaRx / 1024d) / seconds);
                    txKbps = (float) ((deltaTx / 1024d) / seconds);
                }
            }
            
            // Cập nhật state nội bộ (chỉ khi dùng state nội bộ)
            prevNetRx = currentRx;
            prevNetTx = currentTx;
            prevTimestamp = timestamp;
        }

        return new float[]{
            Math.max(0f, rxKbps),
            Math.max(0f, txKbps)
        };
    }


    private String formatBytesPerSec(float bytesPerSec) {
        if (bytesPerSec < 1024) {
            return DF1.format(bytesPerSec) + " bytes/s";
        }
        if (bytesPerSec < 1024 * 1024) {
            return DF1.format(bytesPerSec / 1024f) + " KB/s";
        }
        return DF2.format(bytesPerSec / (1024f * 1024f)) + " MB/s";
    }

    private MetricsViewModel createEmptyViewModel() {
        return new MetricsViewModel(
            0f, "0.0%",
            false, 0f, "not available",
            0f, 0f, "0.0 bytes/s", "0.0 bytes/s"
        );
    }
}

