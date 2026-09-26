package org.example.sstable;

import org.example.index.BloomFilter;
import org.example.index.SparseIndex;

public class SSTableMetadata {

    private final BloomFilter bloomFilter;
    private final SparseIndex sparseIndex;

    public SSTableMetadata(BloomFilter bloomFilter, SparseIndex sparseIndex){
        this.bloomFilter = bloomFilter;
        this.sparseIndex = sparseIndex;
    }

    public BloomFilter bloomFilter(){
        return bloomFilter;
    }

    public SparseIndex sparseIndex(){
        return sparseIndex;
    }
}
