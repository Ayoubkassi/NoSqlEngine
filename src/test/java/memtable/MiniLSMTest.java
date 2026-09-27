package memtable;

import org.example.core.MiniLSM;
import org.example.core.Options;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MiniLSMTest {

    @Test
    void putDeleteFlushRestartGet() throws Exception {

        // Temporary directory for this test
        Path directory = Files.createTempDirectory("minilsm-test");

        // MemTable flushes after every entry.
        // This makes the test deterministic and guarantees
        // that PUT and DELETE both create SSTables.
        Options options = new Options(1);

        // =====================================================
        // STEP 1: PUT
        // =====================================================

        try (MiniLSM db = new MiniLSM(directory, options)) {

            db.put(
                    "user1",
                    "Ayoub".getBytes(StandardCharsets.UTF_8)
            );

            // Value should be immediately readable.
            assertArrayEquals(
                    "Ayoub".getBytes(StandardCharsets.UTF_8),
                    db.get("user1")
            );
        }

        // At this point:
        //
        // WAL
        //  ↓
        // MemTable
        //  ↓
        // FLUSH
        //  ↓
        // SSTable #1
        //
        // SSTable #1:
        //
        // user1 -> Ayoub


        // =====================================================
        // STEP 2: RESTART + DELETE
        // =====================================================

        try (MiniLSM db = new MiniLSM(directory, options)) {

            // Verify that the value survived the restart.
            assertArrayEquals(
                    "Ayoub".getBytes(StandardCharsets.UTF_8),
                    db.get("user1")
            );

            // Delete the key.
            db.delete("user1");

            // The tombstone is in the MemTable.
            // Therefore the value must already be invisible.
            assertNull(
                    db.get("user1")
            );
        }

        // At this point:
        //
        // SSTable #1:
        // user1 -> Ayoub
        //
        // SSTable #2:
        // user1 -> TOMBSTONE


        // =====================================================
        // STEP 3: RESTART AGAIN
        // =====================================================

        try (MiniLSM db = new MiniLSM(directory, options)) {

            // The important test:
            //
            // SSTable #2 contains a tombstone.
            // It is newer than SSTable #1.
            //
            // Therefore SSTable #1's old value must NOT
            // come back.

            assertNull(
                    db.get("user1")
            );
        }
    }

    @Test
    void compactKeepsNewestValueAndTombstone() throws Exception {

        Path directory = Files.createTempDirectory("minilsm-test");

        Options options = new Options(2);

        try (MiniLSM db = new MiniLSM(directory, options)) {

            // First SSTable: user1 = Alice, user2 = Bob
            db.put("user1", "Alice".getBytes(StandardCharsets.UTF_8));
            db.put("user2", "Bob".getBytes(StandardCharsets.UTF_8));

            // Second SSTable: user1 = Ayoub, user2 deleted
            db.put("user1", "Ayoub".getBytes(StandardCharsets.UTF_8));
            db.delete("user2");

            db.compact();

            assertArrayEquals(
                    "Ayoub".getBytes(StandardCharsets.UTF_8),
                    db.get("user1")
            );

            assertNull(db.get("user2"));
        }
    }

    @Test
    void automaticallyCompactsWhenSSTableLimitIsExceeded()
            throws Exception {

        Path directory = Files.createTempDirectory("minilsm-test");

        // Flush after every 2 entries.
        // Allow a maximum of 2 SSTables.
        Options options = new Options(2, 2);

        try (MiniLSM db = new MiniLSM(directory, options)) {

            // SSTable 1
            db.put("user1", "Alice".getBytes(StandardCharsets.UTF_8));
            db.put("user2", "Bob".getBytes(StandardCharsets.UTF_8));

            // SSTable 2
            db.put("user1", "Ayoub".getBytes(StandardCharsets.UTF_8));
            db.put("user3", "Charlie".getBytes(StandardCharsets.UTF_8));

            // SSTable 3 triggers automatic compaction.
            db.put("user4", "David".getBytes(StandardCharsets.UTF_8));
            db.put("user5", "Emma".getBytes(StandardCharsets.UTF_8));

            // The newest value must be preserved.
            assertArrayEquals(
                    "Ayoub".getBytes(StandardCharsets.UTF_8),
                    db.get("user1")
            );

            assertArrayEquals(
                    "Bob".getBytes(StandardCharsets.UTF_8),
                    db.get("user2")
            );

            assertArrayEquals(
                    "Emma".getBytes(StandardCharsets.UTF_8),
                    db.get("user5")
            );
        }

        // Verify that data is still readable after restart.
        try (MiniLSM db = new MiniLSM(directory, options)) {

            assertArrayEquals(
                    "Ayoub".getBytes(StandardCharsets.UTF_8),
                    db.get("user1")
            );

            assertArrayEquals(
                    "Bob".getBytes(StandardCharsets.UTF_8),
                    db.get("user2")
            );

            assertArrayEquals(
                    "Emma".getBytes(StandardCharsets.UTF_8),
                    db.get("user5")
            );
        }
    }
}