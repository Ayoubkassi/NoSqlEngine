package org.example.wal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WriteAheadLog implements AutoCloseable {

    private DataOutputStream out;
    private final Path path;

    public WriteAheadLog(Path path) throws IOException {

        this.path = path;
        Files.createDirectories(path.getParent());

        this.out = new DataOutputStream(
            new BufferedOutputStream(
                    new FileOutputStream(path.toFile(),true)
            )
        );
    }

    public synchronized void put(String key , byte[] val) throws IOException{
        out.writeByte(1); // operation : PUT
        writeString(key);
        out.writeInt(val.length);
        out.write(val);
        out.flush();
    }

    public synchronized void delete(String key) throws IOException {
        out.writeByte(2); //for delete
        writeString(key);
        out.flush();
    }


    // CLEAR THE WALL AFTER IT'S CONTENTS HAVE BEEN
    // SUCCESSFULLY FLUSHED TO AN SSTable
    public synchronized void clear() throws IOException{
        out.flush();
        out.close();

        Files.newOutputStream(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        ).close();

        out = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(path.toFile(), true)
                )
        );
    }

    private void writeString(String value) throws IOException{
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    // the main function is the replay one that take the file and write to the memtable
    public static void replay(String filePath, ReplayConsumer consumer) throws IOException{
        // try with resource
        try(DataInputStream in = new DataInputStream(
                new BufferedInputStream(
                        new FileInputStream(filePath)
                )
        )){
            while(true){
                try{
                    int operation = in.readByte();
                    String key = readString(in);
                    
                    if(operation == 1){
                        int valLength = in.readInt();
                        byte[] val = in.readNBytes(valLength);

                        consumer.accept(new WalEntry(
                                Operation.PUT,
                                key,
                                val
                        ));

                    } else if (operation == 2) {
                        consumer.accept(new WalEntry(
                                Operation.DELETE,
                                key,
                                null
                        ));
                    }
                }catch (EOFException exception){
                    break;
                }
            }
        }
    }
    private static String readString(DataInputStream in) throws  IOException{
        int length = in.readInt();
        byte[]  bytes = in.readNBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
