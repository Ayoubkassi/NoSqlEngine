package org.example.core;

import org.example.memtable.MemTableEntry;
import org.example.memtable.Memtable;
import org.example.sstable.SSTableEntry;
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

public class MiniLSM implements AutoCloseable {

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

        Files.createDirectories(directory);

        this.memTable = new Memtable();
        this.writer = new SSTableWriter();
        this.reader = new SSTableReader();

        Path walPath = directory.resolve("wal.log");
        this.wal = new WriteAheadLog(walPath);

        // Recover previous operations from WAL
        recover();
    }

    private void recover() throws IOException {

        Path walPath = directory.resolve("wal.log");

        if (!Files.exists(walPath)) {
            return;
        }

        WriteAheadLog.replay(
                String.valueOf(walPath),
                entry -> {

                    if (entry.operation() == Operation.PUT) {

                        memTable.put(
                                entry.key(),
                                entry.value()
                        );

                    } else if (entry.operation() == Operation.DELETE) {

                        memTable.delete(
                                entry.key()
                        );
                    }
                }
        );
    }

    public synchronized void put(
            String key,
            byte[] value
    ) throws IOException {

        // 1. Write to WAL first
        wal.put(key, value);

        // 2. Update MemTable
        memTable.put(key, value);

        // 3. Flush if MemTable is full
        if (memTable.size() >= options.memTableMaxEntries()) {
            flush();
        }
    }

    public synchronized byte[] get(String key) throws IOException {

        MemTableEntry entry = memTable.get(key);

        if (entry != null) {

            if (entry.tombstone()) {
                return null;
            }

            return entry.value();
        }

        for (int i = tables.size() - 1; i >= 0; i--) {

            SSTableHandle table = tables.get(i);

            SSTableEntry sstableEntry = reader.get(
                    table.path(),
                    key,
                    table.metadata()
            );

            if (sstableEntry != null) {
                if (sstableEntry.tombstone()) {
                    return null;
                }
                return sstableEntry.value();
            }
        }

        return null;
    }

    public synchronized void delete(String key)
            throws IOException {

        // 1. Write deletion to WAL
        wal.delete(key);

        // 2. Store tombstone in MemTable
        memTable.delete(key);
    }

    private void flush() throws IOException {

        if (memTable.isEmpty()) {
            return;
        }

        Path file = directory.resolve(
                String.format(
                        "sstable-%06d.data",
                        nextTableId++
                )
        );

        // Write MemTable -> SSTable
        SSTableMetadata metadata =
                writer.write(
                        file,
                        memTable.entries()
                );

        // Keep the new SSTable
        tables.add(
                new SSTableHandle(
                        file,
                        metadata
                )
        );

        // Create a fresh MemTable
        memTable = new Memtable();

        // WAL can now be cleared
        wal.clear();
    }

    @Override
    public void close() throws Exception {

        flush();

        wal.close();
    }
}