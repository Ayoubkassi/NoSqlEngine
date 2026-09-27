package org.example.index;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class BloomFilter {

    private final BitSet bits;
    private final int numBits;
    private final int numHashFunctions;

    public BloomFilter(int expectedElements){
        this.numBits = Math.max(1, expectedElements) * 10;
        this.numHashFunctions = 3;
        this.bits = new BitSet(numBits);
    }

    public void add(String key){
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        for(int i =0 ; i < numHashFunctions; i++){
            int hash = hash(bytes,i);
            int index = Math.abs(hash%numBits);
            bits.set(index);
        }
    }

    public boolean mightContain(String key){
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        for(int i=0 ; i < numHashFunctions; i++){
            int hash = hash(bytes,i);
            int index = Math.abs(hash%numBits);

            if(!bits.get(index)){
                return false;
            }
        }

        return true;
    }

    private int hash(byte[] data, int seed){
        int hash = seed;
        for(byte b: data){
            hash = 31*hash+b;
        }
        return hash;
    }
}
