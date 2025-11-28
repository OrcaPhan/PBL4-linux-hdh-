package com.orca.pbl4.ui.process;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class ProcessTableStyleUtil {

    // Màu sắc theo GNOME System Monitor
    private static final Color SELECTION_BACKGROUND = new Color(0x1565C0); // Xanh đậm hơn, dễ nhìn
    private static final Color SELECTION_FOREGROUND = Color.WHITE;
    private static final Color HEADER_BACKGROUND = new Color(0xE8E8E8);
    private static final Color HEADER_FOREGROUND = new Color(0x2E3436);
    private static final Color BORDER_COLOR = new Color(0xD0D0D0);
    private static final Color DEFAULT_TEXT_COLOR = new Color(0x2E3436); // Màu chữ đậm, dễ đọc

    public static void applyTableStyle(JTable table) {
        // Heat-based row coloring - renderer duy nhất cho toàn bảng
        table.setDefaultRenderer(Object.class, new ProcessHeatRenderer(table));
        
        // Selection colors - màu đậm hơn, dễ nhìn
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(SELECTION_FOREGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Màu chữ mặc định cho bảng
        table.setForeground(DEFAULT_TEXT_COLOR);
        
        // Grid - khôi phục viền ô
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setShowGrid(true);
        table.setGridColor(BORDER_COLOR);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // Row height
        table.setRowHeight(22);
        
        // Font
        Font tableFont = new Font("SansSerif", Font.PLAIN, 11);
        table.setFont(tableFont);
        
        // Header style
        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BACKGROUND);
        header.setForeground(HEADER_FOREGROUND);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(header.getWidth(), 28));
        header.setReorderingAllowed(false);
        
        // Header renderer
        header.setDefaultRenderer(new HeaderRenderer());
    }

    private static class ProcessHeatRenderer extends DefaultTableCellRenderer {
        private final JTable table;
        private static final int CPU_COLUMN = 3; // Cột CPU %
        private static final int MEM_COLUMN = 4; // Cột MEM %
        
        public ProcessHeatRenderer(JTable table) {
            this.table = table;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel label = (JLabel) c;
            
            // Đảm bảo màu nền được vẽ đúng
            label.setOpaque(true);
            
            // Format giá trị nếu là CPU% hoặc MEM%
            if (column == CPU_COLUMN && value instanceof Float) {
                label.setText(String.format("%.1f%%", (Float) value));
            } else if (column == MEM_COLUMN && value instanceof Float) {
                label.setText(String.format("%.1f%%", (Float) value));
            }
            
            // Tính màu nền cho cả dòng (một lần cho mỗi row, áp dụng cho tất cả các cột)
            Color rowColor;
            if (isSelected) {
                // Khi hàng được chọn, dùng màu selection mặc định
                rowColor = SELECTION_BACKGROUND;
                label.setForeground(SELECTION_FOREGROUND);
            } else {
                // Tính màu nền dựa trên heat của row
                rowColor = calculateRowColor(row);
                label.setForeground(DEFAULT_TEXT_COLOR); // Màu chữ không đổi
            }
            
            // Áp dụng màu nền cho TẤT CẢ các cột của dòng
            label.setBackground(rowColor);
            
            // Alignment: Name (cột 2) căn trái, còn lại căn phải
            if (column == 2) {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            
            // Border: vừa có grid (matte border) vừa có padding
            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR),
                BorderFactory.createEmptyBorder(0, 6, 0, 6)
            ));
            
            return c;
        }
        
        /**
         * Tính màu nền dựa trên CPU% và MEM% của hàng.
         * Lấy giá trị từ getValueAt, tính heat = max(cpuPercent, memPercent).
         */
        private Color calculateRowColor(int viewRow) {
            try {
                int modelRow = table.convertRowIndexToModel(viewRow);
                
                // Lấy CPU% và MEM% từ model
                Object cpuObj = table.getModel().getValueAt(modelRow, CPU_COLUMN);
                Object memObj = table.getModel().getValueAt(modelRow, MEM_COLUMN);
                
                float cpuPercent = (cpuObj instanceof Float) ? (Float) cpuObj : 0f;
                float memPercent = (memObj instanceof Float) ? (Float) memObj : 0f;
                
                // Trường hợp đặc biệt: CPU hoặc MEM >= 90% → đỏ nhạt
                if (cpuPercent >= 90f || memPercent >= 90f) {
                    return new Color(0xFFCDD2); // Đỏ nhạt
                }
                
                // Tính heat = max(cpuPercent, memPercent)
                float heat = Math.max(cpuPercent, memPercent);
                
                // Áp dụng quy tắc màu theo heat tuyệt đối
                return getHeatColor(heat);
            } catch (Exception e) {
                // Màu mặc định (trắng) nếu có lỗi
                return new Color(0xFFFFFF);
            }
        }
        
        /**
         * Tính màu nền dựa trên heat value tuyệt đối (0.0 - 100.0%).
         * Heat = max(cpuPercent, memPercent).
         */
        private Color getHeatColor(float heat) {
            if (heat == 0.0f) {
                return new Color(0xFFFFFF); // Trắng
            } else if (heat > 0.0f && heat <= 1.0f) {
                return new Color(0xFFFDE7); // Vàng siêu nhạt
            } else if (heat > 1.0f && heat <= 10.0f) {
                return new Color(0xFFF9C4); // Vàng nhạt
            } else if (heat > 10.0f && heat <= 20.0f) {
                return new Color(0xFFE082); // Vàng rõ
            } else if (heat > 20.0f && heat <= 40.0f) {
                return new Color(0xFFECB3); // Cam rất nhạt
            } else if (heat > 40.0f && heat <= 60.0f) {
                return new Color(0xFFCC80); // Cam nhạt
            } else if (heat > 60.0f && heat <= 80.0f) {
                return new Color(0xFFB74D); // Cam đậm
            } else if (heat > 80.0f && heat < 90.0f) {
                return new Color(0xFF9800); // Cam rất đậm
            } else {
                // >= 90%: đã xử lý riêng trong calculateRowColor
                return new Color(0xFF9800);
            }
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        public HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(HEADER_BACKGROUND);
            setForeground(HEADER_FOREGROUND);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            return this;
        }
    }

    public static void applyButtonStyle(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(0xF0F0F0));
        button.setForeground(new Color(0x2E3436));
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.setPreferredSize(new Dimension(80, 28));
        button.setMargin(new Insets(4, 12, 4, 12));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(0xE0E0E0));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(0xF0F0F0));
            }
        });
    }

    public static void applyTextFieldStyle(JTextField textField) {
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        textField.setFont(new Font("SansSerif", Font.PLAIN, 11));
        textField.setPreferredSize(new Dimension(200, 28));
    }

    public static void applyMenuItemStyle(JMenuItem menuItem) {
        menuItem.setFont(new Font("SansSerif", Font.PLAIN, 11));
        menuItem.setPreferredSize(new Dimension(120, 28));
        
        // Hover effect
        menuItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                menuItem.setBackground(new Color(0xE0E0E0));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                menuItem.setBackground(Color.WHITE);
            }
        });
    }
}

