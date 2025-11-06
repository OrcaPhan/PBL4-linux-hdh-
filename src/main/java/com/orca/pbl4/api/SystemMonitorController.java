package com.orca.pbl4.api;

import com.orca.pbl4.core.model.*;
import com.orca.pbl4.core.system.Sampler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller cho System Monitor
 * Cung cấp endpoints để frontend lấy thông tin hệ thống
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Cho phép CORS từ Tauri app
public class SystemMonitorController {

    private final Sampler sampler;

    public SystemMonitorController() {
        this.sampler = new Sampler();
    }

    /**
     * GET /api/memory - Lấy thông tin Memory
     */
    @GetMapping("/memory")
    public ResponseEntity<MemoryInfo> getMemoryInfo() {
        MemoryInfo memInfo = sampler.readMemory();
        return ResponseEntity.ok(memInfo);
    }

    /**
     * GET /api/cpu - Lấy thông tin CPU
     */
    @GetMapping("/cpu")
    public ResponseEntity<CpuInfo> getCpuInfo() {
        CpuInfo cpuInfo = sampler.readCpu();
        return ResponseEntity.ok(cpuInfo);
    }

    /**
     * GET /api/processes - Lấy danh sách processes
     */
    @GetMapping("/processes")
    public ResponseEntity<List<ProcessInfo>> getProcesses(
            @RequestParam(defaultValue = "false") boolean withCmdline) {
        List<ProcessInfo> processes = sampler.readProcesses(withCmdline);
        return ResponseEntity.ok(processes);
    }

    /**
     * GET /api/disk - Lấy thông tin Disk
     */
    @GetMapping("/disk")
    public ResponseEntity<DiskInfo> getDiskInfo() {
        DiskInfo diskInfo = sampler.readDiskTotal();
        return ResponseEntity.ok(diskInfo);
    }

    /**
     * GET /api/network - Lấy thông tin Network
     */
    @GetMapping("/network")
    public ResponseEntity<NetworkInfo> getNetworkInfo() {
        NetworkInfo netInfo = sampler.readNetTotal();
        return ResponseEntity.ok(netInfo);
    }

    /**
     * GET /api/system - Lấy toàn bộ thông tin hệ thống
     */
    @GetMapping("/system")
    public ResponseEntity<SystemSnapshot> getSystemSnapshot(
            @RequestParam(defaultValue = "false") boolean withCmdline) {
        SystemSnapshot snapshot = sampler.readAll(withCmdline);
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ORCA System Monitor API is running");
    }
}
