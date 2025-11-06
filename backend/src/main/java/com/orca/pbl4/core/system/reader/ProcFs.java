package com.orca.pbl4.core.system.reader;

import com.orca.pbl4.core.system.error.SysReadException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProcFs {
    private final Path root;

    public ProcFs() { this(Paths.get("/proc")); }

    public ProcFs(Path root) {
        this.root = Objects.requireNonNull(root);
    }
    public String readString(String first, String... more) {
        try {
            Path p = root.resolve(Paths.get(first, more));
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e){
            throw new SysReadException("Failed to read: " + String.join("/", first, String.join("/", more)), e);
        }
    }
    public List<String> readLines(String first, String... more) {
        try {
            Path p = root.resolve(Paths.get(first, more));
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }catch (IOException e){
            throw new SysReadException("Failed to read lines: " + String.join("/", first, String.join("/", more)), e);
        }

    }
    public List<Integer> listNumericDirs(String first, String... more) {
        Path p = root.resolve(Paths.get(first, more));
        try (
                DirectoryStream<Path> ds = Files.newDirectoryStream(p)
                ){
            List<Integer> list = new ArrayList<>();
            for (Path pa : ds) {
                String name = pa.getFileName().toString();
                if(name.chars().allMatch(Character::isDigit)) {
                    list.add(Integer.parseInt(name));
                }
            }
            return list;
        }catch (IOException e){
            throw new SysReadException("Failed to list numeric dirs in: " + p, e);
        }
    }
    public List<Path> listPaths(String first, String... more) {
        Path p = root.resolve(Paths.get(first, more));
        try(
                DirectoryStream<Path> ds = Files.newDirectoryStream(p)
                ){
            List<Path> list = new ArrayList<>();
            for (Path pa : ds) {
                list.add(pa);
            }
            return list;
        }catch (IOException e){
            throw new SysReadException("Failed to list paths in: " + p, e);
        }
    }
    public String readSymlinkTarget(String first, String... more) {
        try{
            Path p = root.resolve(Paths.get(first, more));
            return Files.readSymbolicLink(p).toString();
        } catch (IOException e) {
            throw new SysReadException("Failed to read symlink: " + String.join("/", first, String.join("/", more)), e);
        }
    }
}
