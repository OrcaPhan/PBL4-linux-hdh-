package com.orca.pbl4.core.system.io;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;


public class ProcReader {
    public List<String> readAllLine(Path p){
        try{
            if(p!=null && Files.isRegularFile(p))
                return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public String readFirstLine(Path p){
        try{
            if(p!=null && Files.isRegularFile(p)){
                try(BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)){
                    return br.readLine();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public String readCmdLine(Path p, int maxBytes){
        try{
            if(p!=null && Files.isRegularFile(p)){
                byte[] raw = Files.readAllBytes(p);
                if(raw.length> maxBytes){
                    byte[]  cut = new byte[maxBytes];
                    System.arraycopy(raw,0,cut,0,raw.length);
                    raw = cut;
                }
                String s = new String (raw, StandardCharsets.UTF_8);
                return s.replace('\0',' ').trim();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
}




