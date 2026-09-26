package org.example;

import org.example.memtable.Memtable;
import org.example.sstable.SSTableMetadata;
import org.example.sstable.SSTableReader;
import org.example.sstable.SSTableWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;


public class Main {
    public static void main(String[] args) throws IOException {

        Memtable memTable = new Memtable();

        memTable.put("user:1", "Ayoub".getBytes());
        memTable.put("user:2", "Sara".getBytes());
        memTable.put("user:3", "John".getBytes());

        Path file = Path.of("/Users/a.kassi/Desktop/NoSQLEngine/data/sstable-00001.data");

        SSTableWriter writer = new SSTableWriter();

        SSTableMetadata metadata =
                writer.write(file, memTable.entries());

        SSTableReader reader = new SSTableReader();

        byte[] result = reader.get(file, "user:2", metadata);

        System.out.println(
                new String(result, StandardCharsets.UTF_8)
        );
    }
}