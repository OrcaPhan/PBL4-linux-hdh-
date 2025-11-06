package com.orca.pbl4.core.system.error;

public class SysReadException extends RuntimeException{
    public SysReadException(String message) { super(message);}
    public SysReadException(String message, Throwable cause) { super(message, cause); }
}
