package org.example.sstable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SSTableWriter {

    public void write(Path file, Map<String, byte[]> entries) throws IOException{
        try(DataOutputStream out = new DataOutputStream(
                Files.newOutputStream(file)
        )){
            for(Map.Entry<String , byte[]> entry : entries.entrySet()){
                byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] val = entry.getValue();

                out.writeInt(key.length);
                out.writeInt(val.length);

                out.write(key);
                out.write(val);
            }
        }
    }
}
