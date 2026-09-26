package org.example.wal;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class WriteAheadLog implements Closeable {
    private final DataOutputStream out;

    public WriteAheadLog(String filePath) throws IOException {
        this.out = new DataOutputStream(
            new BufferedOutputStream(
                    new FileOutputStream(filePath)
            )
        );
    }

    public void put(String key , byte[] val) throws IOException{
        out.writeByte(1); // operation : PUT
        writeString(key);
        out.writeInt(val.length);
        out.write(val);
        out.flush();
    }

    private void writeString(String value) throws IOException{
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public void delete(String key) throws IOException {
        out.writeByte(2); //for delete
        writeString(key);
        out.flush();
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
