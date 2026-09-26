package org.example.sstable;

import java.nio.file.Path;

public record SSTableHandle(
        Path path,
        SSTableMetadata metadata
) {
}
