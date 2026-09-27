package org.example.manifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Manifest {

    private final Path file;

    public Manifest(Path directory) throws IOException{
        this.file = directory.resolve("MANIFEST");

        if(!Files.exists(file))
            Files.createFile(file);
    }

    public synchronized void add(String sstableFile) throws IOException {
        Files.writeString(
                file,
                sstableFile + System.lineSeparator(),
                StandardOpenOption.APPEND
        );
    }

    public synchronized List<String> load() throws IOException {
        if(!Files.exists(file))
            return List.of();

        List<String> lines = Files.readAllLines(file);

        List<String> result = new ArrayList<>();

        for(String line : lines){
            if(!line.isBlank()){
                result.add(line.trim());
            }
        }

        return result;
    }

    public synchronized void rewrite(List<String> sstableFiles) throws IOException {
        Files.write(
                file,
                sstableFiles,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
        );
    }
}
