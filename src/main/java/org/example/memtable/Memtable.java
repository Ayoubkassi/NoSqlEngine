package org.example.memtable;

import java.util.concurrent.ConcurrentSkipListMap;

public class Memtable {

    private final ConcurrentSkipListMap<String, byte[]> data = new ConcurrentSkipListMap<>();

    // we need setter and getter
    public void put(String key , byte[] value){
        data.put(key, value);
    }

    public byte[] get(String key){
        // maybe we need special handle if it does not exists
        return data.get(key);
    }

    public void delete(String key){
        data.remove(key);
    }
    public int size(){
        return data.size();
    }

    public boolean isEmpty(){
        return data.isEmpty();
    }
}
