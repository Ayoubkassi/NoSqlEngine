package org.example.core;

import org.example.memtable.Memtable;
import org.example.sstable.SSTableHandle;
import org.example.sstable.SSTableMetadata;
import org.example.sstable.SSTableReader;
import org.example.sstable.SSTableWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MiniLSM implements AutoCloseable{

    private final Path directory;
    private final Options options;

    private Memtable memTable = new Memtable();

    private final SSTableWriter writer = new SSTableWriter();
    private final SSTableReader reader = new SSTableReader();

    private final List<SSTableHandle> tables = new ArrayList<>();

    private int nextTableId = 1;

    public MiniLSM(Path directory, Options options) throws IOException {
        this.directory = directory;
        this.options = options;

        Files.createDirectory(directory);
    }

    public synchronized void put(String key , byte[] value) throws IOException {
        memTable.put(key,value);
        if(memTable.size() >= options.memTableMaxEntries()){
            flush();
        }
    }

    public synchronized byte[] get(String key) throws IOException {
        byte[] value = memTable.get(key);

        if(value != null)
            return value;

        for(int i =tables.size() -1; i>= 0; i--){
            SSTableHandle table = tables.get(i);

            value = reader.get(
                    table.path(),
                    key,
                    table.metadata()
            );

            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public synchronized void delete(String key){
        memTable.delete(key);

        // Tombstones will be implemented later. (special form so in compaction we remove it)
    }

    private void flush() throws IOException {
        if(memTable.isEmpty())
            return;

        Path file = directory.resolve(String.format("sstable-%06d.data", nextTableId++));

        SSTableMetadata metadata = writer.write(file, memTable.entries());

        tables.add(new SSTableHandle(file, metadata));

        memTable = new Memtable();
    }

    @Override
    public void close() throws Exception {
        flush();
    }
}
