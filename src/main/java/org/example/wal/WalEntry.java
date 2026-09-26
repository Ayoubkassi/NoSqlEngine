package org.example.wal;

public record WalEntry(
    Operation operation,
    String key,
    byte[] value
) {
}
