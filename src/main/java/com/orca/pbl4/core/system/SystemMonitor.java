package com.orca.pbl4.core.system;

import com.orca.pbl4.core.model.*;
import java.util.List;

public class SystemMonitor {
    private final Sampler sampler;


    public SystemMonitor() { this(new Sampler()); }
    public SystemMonitor(Sampler sampler) { this.sampler = sampler; }

    public SystemSnapshot getSystemSnapshot(boolean withCmdline) { return sampler.readAll(withCmdline); }

    public Sampler getSampler() { return sampler; }
}
