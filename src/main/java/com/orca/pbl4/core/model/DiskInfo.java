package com.orca.pbl4.core.model;

public class DiskInfo {
    public long readSectors;
    public long writeSectors;

    public long readBytes(){
        return readSectors*512L ;
    }

    public long writeBytes(){
        return writeSectors*512L ;
    }
}
