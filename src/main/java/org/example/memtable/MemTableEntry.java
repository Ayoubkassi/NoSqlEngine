package org.example.memtable;

public record MemTableEntry(byte[] value, boolean tombstone) {

    public static MemTableEntry value(byte[] value) {
        return new MemTableEntry(value, false);
    }

    public static MemTableEntry deleted() {
        return new MemTableEntry(null, true);
    }
}