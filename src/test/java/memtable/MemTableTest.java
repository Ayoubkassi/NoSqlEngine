package memtable;

import org.example.memtable.Memtable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class MemTableTest {

    @Test
    void shouldStoreAndRetrieveValue(){
        Memtable memTable = new Memtable();
        String key = "user:1";
        memTable.put(key, "Ayoub".getBytes());

        assertArrayEquals("Ayoub".getBytes(), memTable.get(key));
    }

    @Test
    void shouldReturnNullForMissingKey(){
        Memtable memtable = new Memtable();
        assertNull(memtable.get("somekey"));
    }

    @Test
    void shouldDeleteKey(){
        Memtable memtable = new Memtable();
        String key = "user:1";
        memtable.put(key, "Ayoub".getBytes());
        memtable.delete(key);
        assertNull(memtable.get(key));
    }
}
