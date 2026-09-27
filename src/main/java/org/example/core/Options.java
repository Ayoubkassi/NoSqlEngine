package org.example.core;

public record Options(
        int memTableMaxEntries,
        int maxSSTables
) {
    public Options(int memTableMaxEntries){
        this(memTableMaxEntries,4);
    }

    public Options{
        if(memTableMaxEntries <= 0){
            throw new IllegalArgumentException("memTableMaxEntries must be positive");
        }

        if(maxSSTables < 2){
            throw new IllegalArgumentException("maxSStables must be at least 2");
        }
    }

}

