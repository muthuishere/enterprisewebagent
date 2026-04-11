package com.enterprisewebagent.app.persistence;

import com.enterprisewebagent.runtime.tasks.TaskDefinition;
import com.enterprisewebagent.runtime.tasks.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class JpaTaskManagerTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private JpaTaskManager manager;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        sessionRepository.deleteAll();
        manager = new JpaTaskManager(taskRepository);
        // Create sessions that tasks will reference (FK constraint)
        createSession("session-1");
        createSession("session-2");
        createSession("s1");
    }

    private void createSession(String id) {
        var entity = new SessionEntity(id, "ws",
                com.enterprisewebagent.runtime.session.SessionStatus.ACTIVE,
                java.time.Instant.now(), java.time.Instant.now());
        sessionRepository.save(entity);
    }

    @Test
    void createAndGetLifecycle() {
        TaskDefinition task = manager.create("session-1", "Build the feature");

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
        TaskDefinition task = manager.create("s1", "Do something");

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
