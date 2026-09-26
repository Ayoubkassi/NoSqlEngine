package memtable;

import org.example.core.MiniLSM;
import org.example.core.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TombstoneTest {

    @TempDir
    Path dir;

    @Test
    void shouldReturnNullAfterDeleteInMemtable() throws Exception {
        try (MiniLSM lsm = new MiniLSM(dir, new Options(100))) {
            lsm.put("k1", "v1".getBytes());
            lsm.delete("k1");
            assertNull(lsm.get("k1"));
        }
    }

    @Test
    void tombstoneInMemtableShouldShadowSSTable() throws Exception {
        try (MiniLSM lsm = new MiniLSM(dir, new Options(100))) {
            lsm.put("k1", "v1".getBytes());
            for (int i = 0; i < 101; i++) lsm.put("fill" + i, ("f" + i).getBytes());

            byte[] before = lsm.get("k1");
            assertNotNull(before);
            assertArrayEquals("v1".getBytes(), before);

            lsm.delete("k1");
            assertNull(lsm.get("k1"));
        }
    }

    @Test
    void tombstoneShouldPersistInSSTable() throws Exception {
        try (MiniLSM lsm = new MiniLSM(dir, new Options(100))) {
            lsm.put("k1", "v1".getBytes());
            for (int i = 0; i < 101; i++) lsm.put("pad" + i, ("p" + i).getBytes());
            lsm.delete("k1");
            for (int i = 0; i < 101; i++) lsm.put("pad2" + i, ("q" + i).getBytes());
        }

        try (MiniLSM lsm = new MiniLSM(dir, new Options(100))) {
            assertNull(lsm.get("k1"));
        }
    }

    @Test
    void tombstoneShouldSurviveWalRecovery() throws Exception {
        try (MiniLSM lsm = new MiniLSM(dir, new Options(100))) {
            lsm.put("k1", "v1".getBytes());
            lsm.delete("k1");
        }

        try (MiniLSM lsm = new MiniLSM(dir, new Options(100))) {
            assertNull(lsm.get("k1"));
        }
    }
}
