package org.example.index;

import java.util.Map;
import java.util.TreeMap;

public class SparseIndex {

    private final TreeMap<String, Long> index = new TreeMap<>();

    public void add(String key, long offset){
        index.put(key, offset);
    }

    public Long getOffset(String key){
        Map.Entry<String , Long> entry = index.floorEntry(key);
        return entry != null ? entry.getValue() : null;
    }

    public int size(){
        return index.size();
    }
}
