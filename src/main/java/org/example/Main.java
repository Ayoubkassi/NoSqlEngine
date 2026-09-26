package org.example;

import org.example.memtable.Memtable;
import org.example.sstable.SSTableReader;
import org.example.sstable.SSTableWriter;
import org.example.wal.WriteAheadLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        // so now to recover our db we can simple do
//        WriteAheadLog.replay(
//                "./data/wal.log",
//                entry -> {
//                    System.out.println(
//                            entry.operation()
//                            +" "
//                            +entry.key()
//                    );
//                }
//        );

        Memtable memTable = new Memtable();
        memTable.put("user:3", "Me".getBytes());
        memTable.put("user:1", "Ayoub".getBytes());
        memTable.put("user:2", "Kenza".getBytes());

        SSTableWriter writer = new SSTableWriter();
        String fileName = "/Users/a.kassi/Desktop/NoSQLEngine/data/sstable-00001.data";
        writer.write(Path.of(fileName),memTable.entries());

        // and now it will be in other sense

        SSTableReader reader = new SSTableReader();
        byte[] res = reader.get(Path.of(fileName),"user:1");
        System.out.println("res is :"+ new String(res, StandardCharsets.UTF_8));
    }
}