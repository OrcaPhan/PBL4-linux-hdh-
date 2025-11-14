package com.orca.pbl4.core.model;

public class ProcessRow {
    private int pid;             // ID tiến trình
    private String name;         // Tên chương trình hoặc lệnh
    private String user;         // Chủ sở hữu (user)
    private float cpuPercent;    // % CPU đã tính từ delta
    private String memoryStr;    // Dung lượng RAM hiển thị (vd: "512 MB")
    private float memPercent;    // % RAM dùng
    private String diskRead;     // Lượng đọc (vd: "34.6 MB")
    private String diskWrite;    // Lượng ghi (vd: "1.1 GB")
    private String priority;     // "Normal", "Low", "Very Low", ...
    private char state;          // R/S/D/T/Z/I (nếu muốn hiển thị 1 ký tự)

    public ProcessRow() {}
    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public float getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(float cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public String getMemoryStr() {
        return memoryStr;
    }

    public void setMemoryStr(String memoryStr) {
        this.memoryStr = memoryStr;
    }

    public float getMemPercent() {
        return memPercent;
    }

    public void setMemPercent(float memPercent) {
        this.memPercent = memPercent;
    }

    public String getDiskRead() {
        return diskRead;
    }

    public void setDiskRead(String diskRead) {
        this.diskRead = diskRead;
    }

    public String getDiskWrite() {
        return diskWrite;
    }

    public void setDiskWrite(String diskWrite) {
        this.diskWrite = diskWrite;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public char getState() {
        return state;
    }

    public void setState(char state) {
        this.state = state;
    }
}
