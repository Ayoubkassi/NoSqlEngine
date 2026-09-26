package org.example.sstable;

public record SSTableEntry(byte[] value, boolean tombstone) {

    public static SSTableEntry value(byte[] value) {
        return new SSTableEntry(value, false);
    }

    public static SSTableEntry deleted() {
        return new SSTableEntry(null, true);
    }
}
