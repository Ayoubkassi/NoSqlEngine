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
}