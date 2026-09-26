package org.example.core;

public class Options {

    private final int memTableMaxEntries;

    public Options(int memTableMaxEntries){
        this.memTableMaxEntries = memTableMaxEntries;
    }

    public int memTableMaxEntries(){
        return memTableMaxEntries;
    }
}

