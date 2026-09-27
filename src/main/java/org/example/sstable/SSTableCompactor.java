package org.example.sstable;

import org.example.memtable.MemTableEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class SSTableCompactor {

    private final SSTableReader reader;
    private final SSTableWriter writer;

    public SSTableCompactor(SSTableReader reader, SSTableWriter writer){
        this.reader = reader;
        this.writer = writer;
    }

    public SSTableMetadata compact(Path olderFile, Path newFile, Path outputFile) throws IOException {
        Map<String, SSTableEntry> older = reader.readAll(olderFile);
        Map<String, SSTableEntry> newer = reader.readAll(newFile);

        Map<String, SSTableEntry> merged = new TreeMap<>(older);

        merged.putAll(newer);

        Map<String, MemTableEntry> output = new TreeMap<>();

        for(Map.Entry<String, SSTableEntry> entry: merged.entrySet()){
            SSTableEntry value = entry.getValue();
            if(value.tombstone())
                output.put(entry.getKey(), MemTableEntry.deleted());
            else{
                output.put(entry.getKey(), MemTableEntry.value(value.value()));
            }
        }

        return writer.write(outputFile,output);
    }
}
