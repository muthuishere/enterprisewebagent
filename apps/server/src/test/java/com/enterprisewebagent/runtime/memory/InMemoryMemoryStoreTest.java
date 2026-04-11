package com.enterprisewebagent.runtime.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMemoryStoreTest {

    private InMemoryMemoryStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryMemoryStore();
    }

    @Test
    void storeAndRetrieve() {
        var now = Instant.now();
        var entry = new MemoryEntry("key1", "value1", now, now);
        store.store(entry);

        var retrieved = store.retrieve("key1");
        assertTrue(retrieved.isPresent());
        assertEquals("key1", retrieved.get().key());
        assertEquals("value1", retrieved.get().content());
    }

    @Test
    void retrieveNonExistentReturnsEmpty() {
        assertTrue(store.retrieve("missing").isEmpty());
    }

    @Test
    void retrieveRecentReturnsInRecencyOrder() throws InterruptedException {
        var now = Instant.now();
        store.store(new MemoryEntry("a", "first", now, now));
        Thread.sleep(10);
        store.store(new MemoryEntry("b", "second", now, now));
        Thread.sleep(10);
        store.store(new MemoryEntry("c", "third", now, now));

        var recent = store.retrieveRecent(3);
        assertEquals(3, recent.size());
        assertEquals("c", recent.get(0).key());
        assertEquals("b", recent.get(1).key());
        assertEquals("a", recent.get(2).key());
    }

    @Test
    void retrieveRecentRespectsLimit() {
        var now = Instant.now();
        store.store(new MemoryEntry("a", "first", now, now));
        store.store(new MemoryEntry("b", "second", now, now));
        store.store(new MemoryEntry("c", "third", now, now));

        var recent = store.retrieveRecent(2);
        assertEquals(2, recent.size());
    }

    @Test
    void retrieveUpdatesLastAccessed() {
        var past = Instant.now().minusSeconds(100);
        store.store(new MemoryEntry("key1", "val", past, past));

        var retrieved = store.retrieve("key1");
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().lastAccessed().isAfter(past));
    }
}
