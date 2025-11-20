package com.orca.pbl4.service.process;

import com.orca.pbl4.core.model.HandleInfo;
import com.orca.pbl4.core.model.ProcessInfo;
import com.orca.pbl4.core.model.ProcessRow;
import com.orca.pbl4.core.model.ThreadInfo;
import com.orca.pbl4.core.system.SystemSnapshot;

import java.util.List;

/**
 * Contract cho lớp quản lý tiến trình.
 * Cung cấp API tổng hợp + thao tác điều khiển cơ bản trên process.
 */
public interface ProcessManager {

    /**
     * Đọc lại snapshot hệ thống và thông báo cho các listener.
     */
    void refreshSnapshot();

    /**
     * Lấy danh sách ProcessRow đã tính toán từ snapshot mới nhất.
     */
    List<ProcessRow> getAllProcesses();

    /**
     * Áp dụng query (filter + sort + search) trên tập process hiện tại.
     */
    List<ProcessRow> queryProcesses(ProcessQuery query);

    /**
     * Các thao tác điều khiển tiến trình cơ bản.
     */
    boolean killProcess(int pid);
    boolean stopProcess(int pid);
    boolean continueProcess(int pid);
    boolean reniceProcess(int pid, int niceValue);

    /**
     * Lấy thông tin chi tiết của tiến trình.
     */
    ProcessInfo getProcessInfo(int pid);
    List<ThreadInfo> getThreads(int pid);
    List<HandleInfo> getHandles(int pid);

    /**
     * Snapshot thô mới nhất (nếu UI cần số liệu tổng quan).
     */
    SystemSnapshot getCurrentSnapshot();

    void addUpdateListener(ProcessUpdateListener listener);
    void removeUpdateListener(ProcessUpdateListener listener);
}
