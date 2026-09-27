package org.example.core;

import org.example.memtable.MemTableEntry;
import org.example.memtable.Memtable;
import org.example.sstable.*;
import org.example.wal.Operation;
import org.example.wal.WriteAheadLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
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

        recoverSSTables();

        recoverWal();
    }

    private void recoverSSTables() throws IOException {

        List<Path> sstableFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(directory, "sstable-*.data")) {

            for (Path file : stream) {
                sstableFiles.add(file);
            }
        }

        sstableFiles.sort(Path::compareTo);

        for (Path file : sstableFiles) {

            SSTableMetadata metadata = reader.loadMetadata(file);

            tables.add(new SSTableHandle(file, metadata));

            String fileName = file.getFileName().toString();

            String prefix = "sstable-";
            String suffix = ".data";

            String idPart = fileName.substring(
                    prefix.length(),
                    fileName.length() - suffix.length()
            );

            int id = Integer.parseInt(idPart);

            if (id >= nextTableId) {
                nextTableId = id + 1;
            }
        }
    }

    private void recoverWal() throws IOException {

        Path walPath = directory.resolve("wal.log");

        if (!Files.exists(walPath) || Files.size(walPath) == 0) {
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

    public synchronized void compact() throws IOException{
        if(tables.size() < 2){
            return;
        }

        int size = tables.size();

        SSTableHandle older = tables.get(size-2);
        SSTableHandle newer = tables.get(size-1);

        Path outputFile = directory.resolve(
                String.format("sstable-%06d.data",nextTableId++)
        );

        SSTableCompactor compactor = new SSTableCompactor(reader,writer);

        SSTableMetadata metadata = compactor.compact(older.path(),newer.path(),outputFile);

        tables.remove(size-1);
        tables.remove(size-2);

        tables.add(new SSTableHandle(outputFile,metadata));

        Files.deleteIfExists(older.path());
        Files.deleteIfExists(newer.path());
    }

    @Override
    public void close() throws Exception {

        flush();

        wal.close();
    }
}