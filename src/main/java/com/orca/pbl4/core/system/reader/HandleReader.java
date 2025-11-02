package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.model.HandleInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HandleReader {
    private final ProcFs proc;


    public HandleReader() { this(new ProcFs()); }
    public HandleReader(ProcFs proc) { this.proc = proc; }


    public List<HandleInfo> readHandles(int pid) {
        List<Path> fds;
        try { fds = proc.listPaths(String.valueOf(pid), "fd"); }
        catch (Exception e) { return Collections.emptyList(); }


        List<HandleInfo> out = new ArrayList<>();
        for (Path p : fds) {
            String fdName = p.getFileName().toString();
            int fd;
            try { fd = Integer.parseInt(fdName); } catch (NumberFormatException e) { continue; }
            String target;
            try { target = proc.readSymlinkTarget(String.valueOf(pid), "fd", fdName); }
            catch (Exception e) { continue; }
            HandleInfo hi = new HandleInfo(fd,inferType(target),target);
//            hi.setFlags(""); // /proc không cung cấp flags trực tiếp, để trống hoặc mở rộng qua /proc/<pid>/fdinfo
            out.add(hi);
        }
        return out;
    }


    private static String inferType(String target) {
        if (target == null) return "unknown";
        if (target.startsWith("socket:")) return "socket";
        if (target.startsWith("pipe:")) return "pipe";
        if (target.startsWith("anon_inode:")) return "anon_inode";
        if (target.startsWith("/")) return "file";
        return "other";
    }
}
