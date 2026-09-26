package org.example.core;

import org.example.memtable.Memtable;
import org.example.sstable.SSTableHandle;
import org.example.sstable.SSTableMetadata;
import org.example.sstable.SSTableReader;
import org.example.sstable.SSTableWriter;
import org.example.wal.Operation;
import org.example.wal.WriteAheadLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MiniLSM implements AutoCloseable{

    private final Path directory;
    private final Options options;

    private Memtable memTable;
    private final SSTableWriter writer;
    private final SSTableReader reader;
    private final WriteAheadLog wal;

    private final List<SSTableHandle> tables = new ArrayList<>();

    private int nextTableId = 1;

    public MiniLSM(Path directory, Options options) throws IOException {
        this.directory = directory;
        this.options = options;

        Files.createDirectory(directory);

        this.memTable = new Memtable();
        this.writer = new SSTableWriter();
        this.reader = new SSTableReader();

        Path walpath = directory.resolve("wal.log");
        this.wal = new WriteAheadLog(walpath);
    }

    private void recover() throws IOException {
        Path walPath = directory.resolve("wal.log");

        if(!Files.exists(walPath)){
            return;
        }

        WriteAheadLog.replay(
                String.valueOf(walPath),
                entry -> {
                    if( entry.operation() == Operation.PUT ){
                        memTable.put(entry.key(), entry.value());
                    }else if( entry.operation() == Operation.DELETE ){
                        memTable.delete(entry.key());
                    }
                }
        );
    }

    public synchronized void put(String key , byte[] value) throws IOException {

//        always write to wall first
        wal.put(key, value);
//        update memtable
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

    public synchronized void delete(String key) throws IOException {

        wal.delete(key);

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

        wal.clear();
    }

    @Override
    public void close() throws Exception {
        flush();

        wal.close();
    }
}
