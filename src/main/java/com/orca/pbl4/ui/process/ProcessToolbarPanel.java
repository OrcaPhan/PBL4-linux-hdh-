package com.orca.pbl4.ui.process;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;


public class ProcessToolbarPanel extends JPanel {

    private final JTextField txtSearch = new JTextField(20);
    private final JLabel statusLabel = new JLabel();

    private Runnable refreshAction = () -> {};
    private Runnable showMetricsAction = () -> {};
    private Consumer<String> searchConsumer = s -> {};

    public ProcessToolbarPanel() {
        setLayout(new BorderLayout(8, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Color.WHITE);

        // Panel chính: Search + buttons bên trái, Status bên phải
        JPanel mainPanel = new JPanel(new BorderLayout(8, 0));
        mainPanel.setBackground(Color.WHITE);
        
        // Panel bên trái: Search + buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftPanel.setBackground(Color.WHITE);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        leftPanel.add(searchLabel);
        
        ProcessTableStyleUtil.applyTextFieldStyle(txtSearch);
        leftPanel.add(txtSearch);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshAction.run());
        ProcessTableStyleUtil.applyButtonStyle(btnRefresh);
        leftPanel.add(btnRefresh);

        JButton btnMetrics = new JButton("Metrics");
        btnMetrics.addActionListener(e -> showMetricsAction.run());
        ProcessTableStyleUtil.applyButtonStyle(btnMetrics);
        leftPanel.add(btnMetrics);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Status bar bên phải - làm đẹp như một badge/tag trạng thái
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setOpaque(false);
        
        // Tạo panel bao quanh status label để có nền và padding
        JPanel statusContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusContainer.setBackground(new Color(0xE3F2FD)); // Màu nền xanh nhạt
        statusContainer.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12)); // Padding
        statusContainer.setOpaque(true);
        
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12)); // Font đậm, lớn hơn 1pt
        statusLabel.setForeground(new Color(0x1F3B57)); // Màu chữ đậm
        statusLabel.setOpaque(false); // Label không có nền riêng, dùng nền của container
        statusContainer.add(statusLabel);
        
        statusPanel.add(statusContainer);
        mainPanel.add(statusPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { submit(); }
            @Override public void removeUpdate(DocumentEvent e) { submit(); }
            @Override public void changedUpdate(DocumentEvent e) { submit(); }
            private void submit() { searchConsumer.accept(txtSearch.getText()); }
        });

        updateStatus(0, null, null);
    }

    public void updateStatus(int processCount, Integer maxCpuPid, Integer maxMemPid) {
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("Processes: ").append(processCount);
        sb.append(" | ").append(dateTime);
        if (maxCpuPid != null) {
            sb.append(" | Highest CPU: PID ").append(maxCpuPid);
        }
        if (maxMemPid != null) {
            sb.append(" | Highest MEM: PID ").append(maxMemPid);
        }
        statusLabel.setText(sb.toString());
    }

    public void setRefreshAction(Runnable refreshAction) {
        this.refreshAction = refreshAction != null ? refreshAction : () -> {};
    }

    public void setShowMetricsAction(Runnable showMetricsAction) {
        this.showMetricsAction = showMetricsAction != null ? showMetricsAction : () -> {};
    }

    public void setSearchConsumer(Consumer<String> searchConsumer) {
        this.searchConsumer = searchConsumer != null ? searchConsumer : s -> {};
    }
}