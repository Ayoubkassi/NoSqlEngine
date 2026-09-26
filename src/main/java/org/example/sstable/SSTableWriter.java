package org.example.sstable;

import org.example.index.BloomFilter;
import org.example.index.SparseIndex;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SSTableWriter {

    private static final int INDEX_INTERVAL = 100;

    public SSTableMetadata write(Path file, Map<String, byte[]> entries) throws IOException{

        BloomFilter bloomFilter = new BloomFilter(Math.max(1,entries.size()));
        SparseIndex sparseIndex = new SparseIndex();

        long offset = 0;
        int count = 0;

        try(DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(
                    Files.newOutputStream(file)
                )
        )){
            for(Map.Entry<String , byte[]> entry : entries.entrySet()){
                String key = entry.getKey();
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                byte[] value = entry.getValue();


                // Build Bloom Filter
                bloomFilter.add(key);

                // Build Sparse Index
                if (count % INDEX_INTERVAL == 0) {
                    sparseIndex.add(key, offset);
                }

                // Write record
                out.writeInt(keyBytes.length);
                out.writeInt(value.length);
                out.write(keyBytes);
                out.write(value);

                // Update byte offset
                offset += 8L + keyBytes.length + value.length;
                count++;
            }
        }

        return new SSTableMetadata(bloomFilter, sparseIndex);
    }
}
