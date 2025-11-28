package com.orca.pbl4.ui.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.service.process.DefaultProcessManager;
import com.orca.pbl4.service.process.ProcessManager;
import com.orca.pbl4.service.process.ProcessSignalService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

public class ProcessDetailDialog extends JDialog {

    private static final long HZ = 100; // Clock ticks per second (thường là 100 trên Linux)
    private static final long PAGE_SIZE_KB = 4; // Page size 4KB

    private final ProcessManager manager;
    private int pid; // Có thể thay đổi nếu tiến trình đang chọn thay đổi
    private ProcessInfo info;

    private JPanel infoPanel;
    private JTextArea cmdlineArea;
    private JButton btnKill, btnStop, btnContinue, btnSetNice;
    

    private javax.swing.Timer refreshTimer;
    private java.util.function.Supplier<Integer> selectedPidSupplier;

    public ProcessDetailDialog(ProcessManager manager, int pid, java.util.function.Supplier<Integer> selectedPidSupplier) {
        super();
        this.manager = manager;
        this.pid = pid;
        this.selectedPidSupplier = selectedPidSupplier;
        setTitle("Process Detail - PID " + pid);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setModal(false); // Không modal để có thể tương tác với bảng

        loadProcessInfo();
        buildUI();
        
        // Tạo timer để tự refresh mỗi 1 giây
        refreshTimer = new javax.swing.Timer(1000, e -> refreshIfNeeded());
        refreshTimer.start();
        
        // Dừng timer khi đóng dialog
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (refreshTimer != null) {
                    refreshTimer.stop();
                }
            }
        });
    }

    private void loadProcessInfo() {
        // Nếu có supplier, lấy PID đang được chọn trên bảng
        if (selectedPidSupplier != null) {
            Integer selectedPid = selectedPidSupplier.get();
            if (selectedPid != null && selectedPid > 0) {
                pid = selectedPid;
            }
        }
        
        info = manager.getProcessDetail(pid);
        if (info == null) {
            // Process không còn tồn tại, đóng dialog
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            JOptionPane.showMessageDialog(this,
                    "Process with PID " + pid + " not found or cannot be accessed.",
                    "Process Not Found", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    private void refreshIfNeeded() {
        SwingUtilities.invokeLater(() -> {
            // Kiểm tra PID đang được chọn trên bảng (nếu có supplier)
            if (selectedPidSupplier != null) {
                Integer selectedPid = selectedPidSupplier.get();
                if (selectedPid != null && selectedPid > 0 && selectedPid != pid) {
                    // PID đã thay đổi, load lại với PID mới
                    pid = selectedPid;
                    loadProcessInfo();
                    if (info != null) {
                        updateInfoPanel();
                        setTitle("Process Detail - PID " + pid);
                    }
                    return;
                }
            }
            
            // PID không đổi, chỉ refresh thông tin hiện tại
            ProcessInfo newInfo = manager.getProcessDetail(pid);
            if (newInfo == null) {
                // Process không còn tồn tại
                loadProcessInfo(); // Sẽ đóng dialog
                return;
            }
            
            // So sánh các field quan trọng để quyết định có cần update không
            boolean needsUpdate = info == null;
            if (!needsUpdate) {
                // So sánh các field có thể thay đổi
                Float oldCpu = info.getCpuPercent();
                Float newCpu = newInfo.getCpuPercent();
                Float oldMem = info.getMemoryPercent();
                Float newMem = newInfo.getMemoryPercent();
                
                needsUpdate = (oldCpu == null && newCpu != null) || 
                             (oldCpu != null && !oldCpu.equals(newCpu)) ||
                             (oldMem == null && newMem != null) ||
                             (oldMem != null && !oldMem.equals(newMem)) ||
                             info.getProcCpuTicks() != newInfo.getProcCpuTicks() ||
                             info.getRssPages() != newInfo.getRssPages() ||
                             !safe(info.getState(), "").equals(safe(newInfo.getState(), ""));
            }
            
            if (needsUpdate) {
                info = newInfo;
                updateInfoPanel();
                setTitle("Process Detail - PID " + pid);
            }
        });
    }

    private void updateInfoPanel() {
        infoPanel.removeAll();
        fillInfoPanel();
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane())
                .setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Scroll pane cho thông tin chính
        infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Process Information"));
        infoPanel.setBackground(Color.WHITE);
        fillInfoPanel();

        JScrollPane scrollPane = new JScrollPane(infoPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xD0D0D0)));
        add(scrollPane, BorderLayout.CENTER);

        // Panel nút thao tác
        JPanel buttonPanel = createButtonPanel();
        buttonPanel.setBackground(Color.WHITE);
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

        // CPU% - hiển thị giá trị hoặc "N/A" nếu null
        if (info.getCpuPercent() != null) {
            float cpuPercent = info.getCpuPercent();
            addInfoRow("CPU %:", String.format(Locale.US, "%.1f%%", cpuPercent), gbc, row++);
        } else {
            // Hiển thị "N/A" nếu không có giá trị
            addInfoRow("CPU %:", "N/A", gbc, row++);
        }

        // CPU time (từ procCpuTicks)
        double cpuTimeSeconds = info.getProcCpuTicks() / (double) HZ;
        String cpuTimeStr = formatTime(cpuTimeSeconds);
        addInfoRow("CPU time:", cpuTimeStr, gbc, row++);

        // Memory % - hiển thị giá trị hoặc "N/A" nếu null
        if (info.getMemoryPercent() != null) {
            float memPercent = info.getMemoryPercent();
            addInfoRow("Memory %:", String.format(Locale.US, "%.1f%%", memPercent), gbc, row++);
        } else {
            // Hiển thị "N/A" nếu không có giá trị
            addInfoRow("Memory %:", "N/A", gbc, row++);
        }

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
        ProcessTableStyleUtil.applyButtonStyle(btnKill);
        panel.add(btnKill);

        btnStop = new JButton("Stop");
        btnStop.addActionListener(e -> handleStop());
        ProcessTableStyleUtil.applyButtonStyle(btnStop);
        panel.add(btnStop);

        btnContinue = new JButton("Continue");
        btnContinue.addActionListener(e -> handleContinue());
        ProcessTableStyleUtil.applyButtonStyle(btnContinue);
        panel.add(btnContinue);

        btnSetNice = new JButton("Change Priority...");
        btnSetNice.addActionListener(e -> handleSetPriority());
        ProcessTableStyleUtil.applyButtonStyle(btnSetNice);
        panel.add(btnSetNice);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        ProcessTableStyleUtil.applyButtonStyle(btnClose);
        panel.add(btnClose);

        return panel;
    }

    private void handleKill() {
        if (!confirmAction("Kill", "Are you sure you want to kill this process?")) {
            return;
        }
        
        // Kiểm tra xem có cần root không
        boolean requireRoot = shouldRequireRoot();
        sendSignalAsync(pid, "TERM", requireRoot, "Kill", () -> {
            manager.refreshSnapshot();
            dispose();
        });
    }

    private void handleStop() {
        if (!confirmAction("Stop", "Are you sure you want to stop this process?")) {
            return;
        }
        
        boolean requireRoot = shouldRequireRoot();
        sendSignalAsync(pid, "STOP", requireRoot, "Stop", () -> {
            manager.refreshSnapshot();
            refreshInfo();
        });
    }

    private void handleContinue() {
        boolean requireRoot = shouldRequireRoot();
        sendSignalAsync(pid, "CONT", requireRoot, "Continue", () -> {
            manager.refreshSnapshot();
            refreshInfo();
        });
    }

    private void sendSignalAsync(int pid, String signal, boolean requireRoot, String actionName, Runnable onSuccess) {
        ProcessSignalService signalService = getSignalService();
        if (signalService == null) {
            // Fallback: dùng method cũ
            boolean success = switch (signal) {
                case "TERM", "KILL" -> manager.killProcess(pid);
                case "STOP" -> manager.stopProcess(pid);
                case "CONT" -> manager.continueProcess(pid);
                default -> false;
            };
            if (success) {
                if (onSuccess != null) onSuccess.run();
            } else {
                showError(actionName, "Failed to " + actionName.toLowerCase() + " process. Permission denied or process not found.");
            }
            return;
        }

        signalService.sendSignal(pid, signal, requireRoot, result -> {
            switch (result) {
                case SUCCESS -> {
                    if (onSuccess != null) onSuccess.run();
                }
                case PERMISSION_DENIED -> {
                    showError(actionName, "Permission denied. You may need root privileges to " + actionName.toLowerCase() + " this process.");
                }
                case PROCESS_NOT_FOUND -> {
                    showError(actionName, "Process not found. It may have already terminated.");
                }
                case USER_CANCELLED -> {
                    showError(actionName, actionName + " failed: authentication cancelled or wrong password.");
                }
                case UNKNOWN_ERROR -> {
                    showError(actionName, "Unknown error while sending signal.");
                }
            }
        });
    }

    private ProcessSignalService getSignalService() {
        if (manager instanceof DefaultProcessManager) {
            return ((DefaultProcessManager) manager).getSignalService();
        }
        return null;
    }

    private boolean shouldRequireRoot() {
        if (info == null) return false;
        String user = info.getUser();
        if (user == null) return false;
        String currentUser = System.getProperty("user.name");
        // Nếu process thuộc user khác hoặc root thì có thể cần quyền root
        return !user.equals(currentUser) || "root".equals(user);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title + " Error", JOptionPane.ERROR_MESSAGE);
    }

    private void handleSetPriority() {
        // Tạo combo box với các mức priority
        String[] priorities = {
            "Very High",
            "High",
            "Above Normal",
            "Normal",
            "Below Normal",
            "Low",
            "Very Low"
        };
        
        // Lấy priority hiện tại từ process info
        String currentPriority = info != null && info.getPriority() != 0
            ? resolvePriorityFromNice(info.getNice()) 
            : "Normal";
        
        JComboBox<String> comboBox = new JComboBox<>(priorities);
        comboBox.setSelectedItem(currentPriority);
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("Select priority level:"), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Change Priority",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String selectedPriority = (String) comboBox.getSelectedItem();
            if (selectedPriority != null) {
                int niceValue = mapPriorityToNice(selectedPriority);
                boolean requireRoot = shouldRequireRoot();
                
                ProcessSignalService signalService = getSignalService();
                if (signalService != null) {
                    signalService.reniceAsync(pid, niceValue, requireRoot, r -> {
                        switch (r) {
                            case SUCCESS -> {
                                JOptionPane.showMessageDialog(this,
                                    "Priority changed to " + selectedPriority + " successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                                manager.refreshSnapshot();
                                refreshInfo();
                            }
                            case PERMISSION_DENIED -> {
                                showError("Change Priority",
                                    "Permission denied. You may need root privileges.");
                            }
                            case USER_CANCELLED -> {
                                showError("Change Priority",
                                    "Change priority cancelled or wrong password.");
                            }
                            case PROCESS_NOT_FOUND -> {
                                showError("Change Priority",
                                    "Process not found. It may have already terminated.");
                            }
                            case UNKNOWN_ERROR -> {
                                showError("Change Priority",
                                    "Unknown error while changing priority.");
                            }
                        }
                    });
                } else {
                    // Fallback: dùng method cũ
                    if (manager.reniceProcess(pid, niceValue)) {
                        JOptionPane.showMessageDialog(this,
                                "Priority changed to " + selectedPriority + " successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        manager.refreshSnapshot();
                        refreshInfo();
                    } else {
                        showError("Change Priority",
                            "Failed to change priority. You may not have permission.");
                    }
                }
            }
        }
    }

    private int mapPriorityToNice(String priority) {
        return switch (priority) {
            case "Very High" -> -15;
            case "High" -> -10;
            case "Above Normal" -> -5;
            case "Normal" -> 0;
            case "Below Normal" -> 5;
            case "Low" -> 10;
            case "Very Low" -> 15;
            default -> 0;
        };
    }

    private String resolvePriorityFromNice(int nice) {
        if (nice <= -15) return "Very High";
        if (nice <= -10) return "High";
        if (nice < 0) return "Above Normal";
        if (nice == 0) return "Normal";
        if (nice <= 5) return "Below Normal";
        if (nice <= 10) return "Low";
        return "Very Low";
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
            updateInfoPanel();
        }
    }

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
