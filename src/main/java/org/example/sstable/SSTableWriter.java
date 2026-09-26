package org.example.sstable;

import org.example.index.BloomFilter;
import org.example.index.SparseIndex;
import org.example.memtable.MemTableEntry;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SSTableWriter {

    private static final int INDEX_INTERVAL = 100;

    public SSTableMetadata write(
            Path file,
            Map<String, MemTableEntry> entries
    ) throws IOException {

        BloomFilter bloomFilter =
                new BloomFilter(Math.max(1, entries.size()));

        SparseIndex sparseIndex = new SparseIndex();

        long offset = 0;
        int count = 0;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(
                        Files.newOutputStream(file)
                )
        )) {

            for (Map.Entry<String, MemTableEntry> entry : entries.entrySet()) {

                String key = entry.getKey();
                MemTableEntry memTableEntry = entry.getValue();

                byte[] keyBytes =
                        key.getBytes(StandardCharsets.UTF_8);

                // -------------------------
                // Bloom Filter
                // -------------------------
                bloomFilter.add(key);

                // -------------------------
                // Sparse Index
                // -------------------------
                if (count % INDEX_INTERVAL == 0) {
                    sparseIndex.add(key, offset);
                }

                // -------------------------
                // Record
                // -------------------------

                // key length
                out.writeInt(keyBytes.length);

                // value length
                if (memTableEntry.tombstone()) {
                    out.writeInt(0);
                } else {
                    out.writeInt(memTableEntry.value().length);
                }

                // tombstone flag
                out.writeBoolean(memTableEntry.tombstone());

                // key
                out.write(keyBytes);

                // value
                if (!memTableEntry.tombstone()) {
                    out.write(memTableEntry.value());
                }

                // -------------------------
                // Update offset
                // -------------------------

                offset +=
                        4L +                         // key length
                                4L +                         // value length
                                1L +                         // tombstone flag
                                keyBytes.length +
                                (memTableEntry.tombstone()
                                        ? 0
                                        : memTableEntry.value().length);

                count++;
            }
        }

        return new SSTableMetadata(
                bloomFilter,
                sparseIndex
        );
    }
}