package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.service.process.ProcessManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Dialog hiển thị chi tiết đầy đủ của một tiến trình.
 * Đọc dữ liệu từ /proc qua ProcessManager.getProcessDetail().
 */
public class ProcessDetailDialog extends JDialog {

    private static final long HZ = 100; // Clock ticks per second (thường là 100 trên Linux)
    private static final long PAGE_SIZE_KB = 4; // Page size 4KB

    private final ProcessManager manager;
    private final int pid;
    private ProcessInfo info;

    // UI components
    private JPanel infoPanel;
    private JTextArea cmdlineArea;
    private JButton btnKill, btnStop, btnContinue, btnSetNice;

    public ProcessDetailDialog(ProcessManager manager, int pid) {
        super();
        this.manager = manager;
        this.pid = pid;
        setTitle("Process Detail - PID " + pid);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setModal(true);

        loadProcessInfo();
        buildUI();
    }

    private void loadProcessInfo() {
        info = manager.getProcessDetail(pid);
        if (info == null) {
            JOptionPane.showMessageDialog(this,
                    "Process with PID " + pid + " not found or cannot be accessed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
//        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        ((JComponent) getContentPane())
                .setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Scroll pane cho thông tin chính
        infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Process Information"));
        fillInfoPanel();

        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // Panel nút thao tác
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void fillInfoPanel() {
        if (info == null) return;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 4, 8);
        int row = 0;

        // Process name
        addInfoRow("Process name:", safe(info.getName(), "?"), gbc, row++);

        // User
        addInfoRow("User:", safe(info.getUser(), "?"), gbc, row++);

        // State
        addInfoRow("State:", formatState(info.getState()), gbc, row++);

        // CPU%
        float cpuPercent = info.getCpuPercent() != null ? info.getCpuPercent() : 0f;
        addInfoRow("CPU %:", String.format(Locale.US, "%.2f%%", cpuPercent), gbc, row++);

        // CPU time (từ procCpuTicks)
        double cpuTimeSeconds = info.getProcCpuTicks() / (double) HZ;
        String cpuTimeStr = formatTime(cpuTimeSeconds);
        addInfoRow("CPU time:", cpuTimeStr, gbc, row++);

        // Memory %
        float memPercent = info.getMemoryPercent() != null ? info.getMemoryPercent() : 0f;
        addInfoRow("Memory (%):", String.format(Locale.US, "%.2f%%", memPercent), gbc, row++);

        // RSS
        long rssKB = info.getRssPages() * PAGE_SIZE_KB;
        addInfoRow("RSS:", formatMemory(rssKB), gbc, row++);

        // Virtual memory
        if (info.getVirtualPages() > 0) {
            long virtualKB = info.getVirtualPages() * PAGE_SIZE_KB;
            addInfoRow("Virtual memory:", formatMemory(virtualKB), gbc, row++);
        }

        // Shared memory
        if (info.getSharedPages() > 0) {
            long sharedKB = info.getSharedPages() * PAGE_SIZE_KB;
            addInfoRow("Shared memory:", formatMemory(sharedKB), gbc, row++);
        }

        // Nice
        addInfoRow("Nice:", String.valueOf(info.getNice()), gbc, row++);

        // Priority
        addInfoRow("Priority:", String.valueOf(info.getPriority()), gbc, row++);

        // Started (từ startTimeTicks)
        String startedStr = formatStartTime(info.getStartTimeTicks());
        addInfoRow("Started:", startedStr, gbc, row++);

        // PID
        addInfoRow("PID:", String.valueOf(info.getPid()), gbc, row++);

        // Threads
        List<ThreadInfo> threads = info.getThreads();
        addInfoRow("Threads:", String.valueOf(threads != null ? threads.size() : 0), gbc, row++);

        // Handles
        List<HandleInfo> handles = info.getHandles();
        addInfoRow("Handles:", String.valueOf(handles != null ? handles.size() : 0), gbc, row++);

        // I/O Read
        if (info.getIoReadBytes() > 0) {
            addInfoRow("I/O Read:", formatBytes(info.getIoReadBytes()), gbc, row++);
        }

        // I/O Write
        if (info.getIoWriteBytes() > 0) {
            addInfoRow("I/O Write:", formatBytes(info.getIoWriteBytes()), gbc, row++);
        }

        // Command line
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        infoPanel.add(new JLabel("Command line:"), gbc);

        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        cmdlineArea = new JTextArea(safe(info.getCmdline(), "(no command line)"));
        cmdlineArea.setEditable(false);
        cmdlineArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        cmdlineArea.setBackground(Color.WHITE);
        cmdlineArea.setBorder(BorderFactory.createLoweredBevelBorder());
        JScrollPane cmdlineScroll = new JScrollPane(cmdlineArea);
        cmdlineScroll.setPreferredSize(new Dimension(0, 100));
        infoPanel.add(cmdlineScroll, gbc);
    }

    private void addInfoRow(String label, String value, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        infoPanel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(new JLabel(value), gbc);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));

        btnKill = new JButton("Kill");
        btnKill.addActionListener(e -> handleKill());
        panel.add(btnKill);

        btnStop = new JButton("Stop");
        btnStop.addActionListener(e -> handleStop());
        panel.add(btnStop);

        btnContinue = new JButton("Continue");
        btnContinue.addActionListener(e -> handleContinue());
        panel.add(btnContinue);

        btnSetNice = new JButton("Set Nice...");
        btnSetNice.addActionListener(e -> handleSetNice());
        panel.add(btnSetNice);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);

        return panel;
    }

    private void handleKill() {
        if (confirmAction("Kill", "Are you sure you want to kill this process?")) {
            if (manager.killProcess(pid)) {
                JOptionPane.showMessageDialog(this, "Process killed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                manager.refreshSnapshot();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to kill process. You may not have permission.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleStop() {
        if (confirmAction("Stop", "Are you sure you want to stop this process?")) {
            if (manager.stopProcess(pid)) {
                JOptionPane.showMessageDialog(this, "Process stopped successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                manager.refreshSnapshot();
                refreshInfo();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to stop process. You may not have permission.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleContinue() {
        if (manager.continueProcess(pid)) {
            JOptionPane.showMessageDialog(this, "Process continued successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            manager.refreshSnapshot();
            refreshInfo();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to continue process. You may not have permission.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSetNice() {
        String input = JOptionPane.showInputDialog(this,
                "Enter new nice value (-20 to 19):",
                "Set Nice",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) return; // User cancelled

        try {
            int niceValue = Integer.parseInt(input.trim());
            if (niceValue < -20 || niceValue > 19) {
                JOptionPane.showMessageDialog(this,
                        "Nice value must be between -20 and 19.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (manager.reniceProcess(pid, niceValue)) {
                JOptionPane.showMessageDialog(this,
                        "Nice value set successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                manager.refreshSnapshot();
                refreshInfo();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to set nice value. You may not have permission.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid number format.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean confirmAction(String action, String message) {
        return JOptionPane.showConfirmDialog(this,
                message,
                action + " Process",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void refreshInfo() {
        loadProcessInfo();
        if (info != null) {
            infoPanel.removeAll();
            fillInfoPanel();
            infoPanel.revalidate();
            infoPanel.repaint();
        }
    }

    // Format helpers
    private String formatState(String state) {
        if (state == null || state.isEmpty()) return "?";
        char s = state.charAt(0);
        return switch (s) {
            case 'R' -> "R (Running)";
            case 'S' -> "S (Sleeping)";
            case 'D' -> "D (Disk sleep)";
            case 'T' -> "T (Stopped)";
            case 'Z' -> "Z (Zombie)";
            default -> String.valueOf(s);
        };
    }

    private String formatTime(double seconds) {
        if (seconds < 60) {
            return String.format(Locale.US, "%.2f s", seconds);
        } else if (seconds < 3600) {
            return String.format(Locale.US, "%.2f m", seconds / 60);
        } else {
            return String.format(Locale.US, "%.2f h", seconds / 3600);
        }
    }

    private String formatMemory(long kb) {
        if (kb < 1024) {
            return String.format(Locale.US, "%.2f KiB", (double) kb);
        } else if (kb < 1024 * 1024) {
            return String.format(Locale.US, "%.2f MiB", kb / 1024.0);
        } else {
            return String.format(Locale.US, "%.2f GiB", kb / (1024.0 * 1024.0));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.2f KiB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024L) {
            return String.format(Locale.US, "%.2f MiB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.US, "%.2f GiB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Format start time từ startTimeTicks.
     * startTimeTicks là số ticks từ boot, cần đọc /proc/uptime để tính thời gian thực.
     * Để đơn giản, hiển thị relative time hoặc absolute nếu có thể.
     */
    private String formatStartTime(long startTimeTicks) {
        try {
            // Đọc /proc/uptime để tính boot time
            java.nio.file.Path uptimePath = java.nio.file.Paths.get("/proc/uptime");
            String uptimeStr = java.nio.file.Files.readString(uptimePath);
            double uptimeSeconds = Double.parseDouble(uptimeStr.split("\\s+")[0]);
            long bootTimeSeconds = (long) (System.currentTimeMillis() / 1000 - uptimeSeconds);
            
            // startTime = bootTime + (startTimeTicks / HZ)
            long startTimeSeconds = bootTimeSeconds + (startTimeTicks / HZ);
            LocalDateTime startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(startTimeSeconds),
                    ZoneId.systemDefault());
            
            return startTime.toString().replace('T', ' ');
        } catch (Exception e) {
            // Fallback: hiển thị ticks
            return startTimeTicks + " ticks (from boot)";
        }
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
