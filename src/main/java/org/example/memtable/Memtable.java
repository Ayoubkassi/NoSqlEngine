package org.example.memtable;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class Memtable {

    private final ConcurrentSkipListMap<String, MemTableEntry> data =
            new ConcurrentSkipListMap<>();

    public void put(String key, byte[] value) {
        data.put(key, MemTableEntry.value(value));
    }

    public MemTableEntry get(String key) {
        return data.get(key);
    }

    public void delete(String key) {
        data.put(key, MemTableEntry.deleted());
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Map<String, MemTableEntry> entries() {
        return data;
    }
}