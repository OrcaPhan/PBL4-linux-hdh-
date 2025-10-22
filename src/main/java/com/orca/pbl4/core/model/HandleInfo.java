package com.orca.pbl4.core.model;

public class HandleInfo {
    private final int fd;
    private final String type;
    private final String target;
    private final String flags;

    public HandleInfo(int fd, String type, String target) {
    this(fd, type, target, "");
    }
    public HandleInfo(int fd, String type, String target, String flags) {
        this.fd = fd;
        this.type = type == null ? "" : type;
        this.target = target == null ? "" : target;
        this.flags = flags == null ? "" : flags;
    }
    //getter
    public int getFd() {
        return fd;
    }
    public String getType() {
        return type;
    }
    public String getTarget() {
        return target;
    }
    public String getFlags() {
        return flags;
    }
    public String toString() {
        return "HandleInfo{ fd=" + fd + ", type=" + type + ", target=" + target + ", flags=" + flags + " }";
    }
}
