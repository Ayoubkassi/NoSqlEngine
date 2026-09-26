package org.example.sstable;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SSTableReader {

    public byte[] get(Path file, String targetKey) throws IOException{
        try(DataInputStream in = new DataInputStream(
                Files.newInputStream(file)
        )){
            while(true){
                try{
                    int keyLength = in.readInt();
                    int valLength = in.readInt();

                    byte[] keyBytes = in.readNBytes(keyLength);
                    byte[] val = in.readNBytes(valLength);

                    String key = new String(keyBytes, StandardCharsets.UTF_8);
                    if(key.equals(targetKey)){
                        return val;
                    }
                }catch (EOFException exception){
                    return null;
                }
            }
        }
    }
}
