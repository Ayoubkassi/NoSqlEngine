package org.example;

import org.example.core.MiniLSM;
import org.example.core.Options;
import org.example.memtable.Memtable;
import org.example.sstable.SSTableMetadata;
import org.example.sstable.SSTableReader;
import org.example.sstable.SSTableWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;


public class Main {
    public static void main(String[] args) throws Exception {

        Path directory = Path.of("/Users/a.kassi/Desktop/NoSQLEngine/data");

        Options options = new Options(3);

        try (MiniLSM db = new MiniLSM(directory, options)) {

            db.put("user:1", "Ayoub".getBytes());
            db.put("user:2", "Sara".getBytes());
            db.put("user:3", "John".getBytes());

            byte[] value = db.get("user:1");

            System.out.println(
                    new String(value, StandardCharsets.UTF_8)
            );
        }
    }
}