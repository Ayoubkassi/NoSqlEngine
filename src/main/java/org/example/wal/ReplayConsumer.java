package org.example.wal;

@FunctionalInterface
public interface ReplayConsumer {
    void accept(WalEntry entry);
}
