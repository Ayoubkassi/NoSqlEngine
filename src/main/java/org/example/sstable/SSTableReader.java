package org.example.sstable;

import org.example.index.BloomFilter;
import org.example.index.SparseIndex;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class SSTableReader {

    public SSTableMetadata loadMetadata(Path file) throws IOException {

        BloomFilter bloomFilter = new BloomFilter(1);
        SparseIndex sparseIndex = new SparseIndex();

        long offset = 0;
        int count = 0;

        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {

            while (in.getFilePointer() < in.length()) {

                int keyLength = in.readInt();
                int valueLength = in.readInt();
                boolean isTombstone = in.readBoolean();

                byte[] keyBytes = new byte[keyLength];
                in.readFully(keyBytes);

                if (!isTombstone) {
                    in.skipBytes(valueLength);
                }

                String key = new String(keyBytes, StandardCharsets.UTF_8);

                bloomFilter.add(key);

                if (count % 100 == 0) {
                    sparseIndex.add(key, offset);
                }

                offset +=
                        4L +
                                4L +
                                1L +
                                keyLength +
                                (isTombstone ? 0 : valueLength);

                count++;
            }
        }

        int numEntries = Math.max(1, count);
        bloomFilter = new BloomFilter(numEntries);

        offset = 0;
        count = 0;

        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {

            while (in.getFilePointer() < in.length()) {

                int keyLength = in.readInt();
                int valueLength = in.readInt();
                boolean isTombstone = in.readBoolean();

                byte[] keyBytes = new byte[keyLength];
                in.readFully(keyBytes);

                if (!isTombstone) {
                    in.skipBytes(valueLength);
                }

                String key = new String(keyBytes, StandardCharsets.UTF_8);

                bloomFilter.add(key);

                if (count % 100 == 0) {
                    sparseIndex.add(key, offset);
                }

                offset +=
                        4L +
                                4L +
                                1L +
                                keyLength +
                                (isTombstone ? 0 : valueLength);

                count++;
            }
        }

        return new SSTableMetadata(bloomFilter, sparseIndex);
    }

    public SSTableEntry get(Path file, String targetKey, SSTableMetadata metadata) throws IOException{

        // Step 1: Check Bloom Filter
        if (!metadata.bloomFilter().mightContain(targetKey)) {
            return null;
        }

        // Step 2: Find starting offset
        SparseIndex index = metadata.sparseIndex();

        Long offset = index.getOffset(targetKey);

        if (offset == null) {
            return null;
        }

        // Step 3: Scan from the indexed position
        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {

            in.seek(offset);

            while (in.getFilePointer() < in.length()) {

                int keyLength = in.readInt();
                int valueLength = in.readInt();
                boolean isTombstone = in.readBoolean();

                byte[] keyBytes = new byte[keyLength];
                byte[] value = new byte[valueLength];

                in.readFully(keyBytes);
                in.readFully(value);

                String key = new String(keyBytes, StandardCharsets.UTF_8);

                int comparison = key.compareTo(targetKey);

                if (comparison == 0) {
                    return isTombstone
                            ? SSTableEntry.deleted()
                            : SSTableEntry.value(value);
                }

                // Keys are sorted, so we can stop early
                if (comparison > 0) {
                    return null;
                }
            }
        }

        return null;
    }
}
