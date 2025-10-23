package com.orca.pbl4.core.system.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassWdCache {
    private final Map<Integer, String> uidToName = new HashMap<>();
    private boolean loaded = false;

    public synchronized void refreshIfEmpty() {
        if (loaded) return;
        Path p = Paths.get("/etc/passwd");

        try{
            List<String> lines = Files.readAllLines(p);
            for(String line : lines){
                if(line.startsWith("#") || line.isBlank()){ continue; }
                String[] parts = line.split(":");
                if(parts.length >=3){
                    String name = parts[0].trim();
                    try{
                        int uid = Integer.parseInt(parts[2].trim());
                        uidToName.put(uid, name);
                    }
                    catch(NumberFormatException ignored){}
                }
            }
            loaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String usernameOff(String uidLine){
        if(uidLine == null) return null;
        String[] parts = uidLine.split("\\s+");
        if(parts.length == 0) return null;
        try{
            int uid = Integer.parseInt(parts[0].trim());
            refreshIfEmpty();
            return uidToName.getOrDefault(uid, String.valueOf(uid));
        }
        catch(NumberFormatException ignored){
            return null;
        }
    }
}
