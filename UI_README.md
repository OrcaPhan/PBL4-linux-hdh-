# ORCA System Monitor - JavaSwing UI

Ứng dụng giám sát hệ thống Linux với giao diện JavaSwing, tương tự Task Manager/System Monitor.

## 📁 Cấu trúc UI

```
src/main/java/com/orca/pbl4/
├── App.java                          # Entry point - Chạy ứng dụng
└── ui/
    ├── MainFrame.java                # JFrame chính với tabs
    ├── panels/
    │   ├── ProcessesPanel.java       # Tab hiển thị danh sách processes
    │   └── PerformancePanel.java     # Tab hiển thị biểu đồ CPU/Memory
    ├── dialogs/
    │   └── ProcessDetailsDialog.java # Dialog chi tiết process
    ├── components/
    │   ├── ProcessTableModel.java    # TableModel cho JTable
    │   ├── SystemInfoPanel.java      # Panel tổng quan CPU/RAM
    │   ├── CpuChartPanel.java        # Vẽ biểu đồ CPU
    │   └── MemoryChartPanel.java     # Vẽ biểu đồ Memory
    └── utils/
        ├── DataRefresher.java        # Timer refresh data
        └── FormatUtils.java          # Format KB→MB→GB
```

## 🚀 Cách chạy

### Trên Ubuntu/Linux (khuyên dùng):

```bash
# Compile
mvn clean compile

# Chạy GUI
mvn exec:java -Dexec.mainClass="com.orca.pbl4.App"

# Hoặc dùng java trực tiếp
java -cp target/classes com.orca.pbl4.App
```

### Trên WSL2 (Windows Subsystem for Linux):

```bash
# 1. Mở WSL
wsl

# 2. Cài X server trên Windows (để hiển thị GUI)
# Download VcXsrv hoặc Xming

# 3. Trong WSL, set DISPLAY
export DISPLAY=:0

# 4. Chạy app
cd /mnt/e/PBL4/PBL4-linux-hdh-
mvn exec:java -Dexec.mainClass="com.orca.pbl4.App"
```

### ⚠️ Lưu ý:
- **KHÔNG chạy được trực tiếp trên Windows** (cần `/proc` filesystem của Linux)
- Phải chạy trên Linux hoặc WSL2

## 📊 Tính năng UI

### 1. Tab "Processes"
- ✅ Bảng danh sách processes với cột: PID, Name, User, %CPU, %MEM, RSS, State, Nice
- ✅ Sort theo từng cột (click header)
- ✅ Search/Filter processes
- ✅ Kill process (right click hoặc button)
- ✅ Double click để xem chi tiết
- ✅ Auto refresh mỗi 1 giây
- ✅ Progress bar hiển thị CPU và RAM tổng

### 2. Tab "Performance"
- ✅ Biểu đồ CPU usage theo thời gian (60s history)
- ✅ Biểu đồ Memory usage theo thời gian
- ✅ Hiển thị Disk I/O (Read/Write MB/s)
- ✅ Hiển thị Network (RX/TX MB/s)

### 3. Dialog "Process Details"
- ✅ Thông tin chi tiết process (PID, User, State, Priority, etc.)
- ✅ Danh sách Threads
- ✅ Danh sách File Handles/FDs
- ✅ I/O Statistics (Read/Write bytes)
- ✅ Command line arguments

## 🎨 Tùy chỉnh

### Thay đổi refresh interval:
```java
// Trong ProcessesPanel.java hoặc PerformancePanel.java
refresher = new DataRefresher(2000, this::updateData); // 2 giây thay vì 1
```

### Thay đổi số lượng data points trong chart:
```java
// Trong CpuChartPanel.java hoặc MemoryChartPanel.java
private static final int MAX_DATA_POINTS = 120; // 120 giây thay vì 60
```

### Thay đổi màu sắc:
```java
// Trong SystemInfoPanel.java
if (cpuPercent > 80) {
    cpuProgressBar.setForeground(Color.RED);
} else if (cpuPercent > 50) {
    cpuProgressBar.setForeground(Color.ORANGE);
} else {
    cpuProgressBar.setForeground(Color.GREEN);
}
```

## 🔧 Mở rộng

### Thêm tab mới:
1. Tạo class extends `JPanel` trong `ui/panels/`
2. Implement logic trong panel
3. Add vào `MainFrame.java`:
```java
MyNewPanel myPanel = new MyNewPanel(systemMonitor);
tabbedPane.addTab("My Tab", myPanel);
```

### Thêm cột mới vào Process Table:
1. Sửa `ProcessTableModel.java`:
   - Thêm column name vào `columnNames[]`
   - Thêm case mới trong `getValueAt()`
2. Sửa column width trong `ProcessesPanel.createProcessTable()`

## 📝 TODO (Tính năng có thể thêm)

- [ ] Export process list to CSV
- [ ] Process tree view (parent-child hierarchy)
- [ ] CPU per-core usage
- [ ] Disk usage per partition
- [ ] Network per interface
- [ ] System uptime
- [ ] Load average (1m, 5m, 15m)
- [ ] Search processes by PID
- [ ] Change process priority (renice)
- [ ] Theme switcher (Dark mode)

## 🐛 Troubleshooting

### Lỗi "NoSuchFileException: /proc/stat"
→ Đang chạy trên Windows. Phải dùng WSL2 hoặc Linux.

### GUI không hiển thị trên WSL2
→ Cần cài X Server (VcXsrv) và set `DISPLAY=:0`

### Lỗi "kill: permission denied"
→ Chỉ có thể kill processes của user hiện tại, hoặc chạy với sudo

## 📚 Tài liệu tham khảo

- JavaSwing Tutorial: https://docs.oracle.com/javase/tutorial/uiswing/
- Linux /proc filesystem: https://man7.org/linux/man-pages/man5/proc.5.html
