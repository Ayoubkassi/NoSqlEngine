package org.example;

import org.example.wal.WriteAheadLog;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        // so now to recover our db we can simple do
        WriteAheadLog.replay(
                "./data/wal.log",
                entry -> {
                    System.out.println(
                            entry.operation()
                            +" "
                            +entry.key()
                    );
                }
        );
    }
}