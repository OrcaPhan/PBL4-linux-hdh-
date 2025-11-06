package com.orca.pbl4.core.model;

public class DiskInfo {
    public final long readSectors;
    public final long writeSectors;

    public DiskInfo(long readSectors, long writeSectors) {
        this.readSectors = readSectors;
        this.writeSectors = writeSectors;
    }

    public long readBytes(){
        return readSectors*512L ;
    }

    public long writeBytes(){
        return writeSectors*512L ;
    }
}
