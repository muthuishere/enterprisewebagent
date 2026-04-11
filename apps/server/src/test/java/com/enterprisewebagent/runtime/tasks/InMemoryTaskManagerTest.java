package com.enterprisewebagent.runtime.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {

    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void createAndGetLifecycle() {
        var task = manager.create("session-1", "Build the feature");

        assertNotNull(task.id());
        assertEquals("session-1", task.sessionId());
        assertEquals("Build the feature", task.description());
        assertEquals(TaskStatus.PENDING, task.status());

        var retrieved = manager.get(task.id());
        assertTrue(retrieved.isPresent());
        assertEquals(task.id(), retrieved.get().id());
    }

    @Test
    void updateStatusLifecycle() {
        var task = manager.create("s1", "Do something");

        manager.updateStatus(task.id(), TaskStatus.RUNNING);
        assertEquals(TaskStatus.RUNNING, manager.get(task.id()).orElseThrow().status());

        manager.updateStatus(task.id(), TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, manager.get(task.id()).orElseThrow().status());
    }

    @Test
    void getNonExistentReturnsEmpty() {
        assertTrue(manager.get("missing").isEmpty());
    }

    @Test
    void listBySessionFiltersCorrectly() {
        manager.create("session-1", "Task A");
        manager.create("session-1", "Task B");
        manager.create("session-2", "Task C");

        var s1Tasks = manager.listBySession("session-1");
        assertEquals(2, s1Tasks.size());

        var s2Tasks = manager.listBySession("session-2");
        assertEquals(1, s2Tasks.size());

        var s3Tasks = manager.listBySession("session-3");
        assertTrue(s3Tasks.isEmpty());
    }

    @Test
    void updateStatusNonExistentThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.updateStatus("missing", TaskStatus.RUNNING));
    }
}
