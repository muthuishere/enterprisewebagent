package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.tasks.TaskDefinition;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tasks.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskManager taskManager;

    private final TaskDefinition testTask = new TaskDefinition(
            "task-1", "sess-1", "do something", TaskStatus.PENDING,
            Instant.parse("2025-01-01T00:00:00Z"));

    @Test
    void createTask_returnsTask() throws Exception {
        when(taskManager.create("sess-1", "do something")).thenReturn(testTask);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"sess-1\",\"description\":\"do something\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-1"))
                .andExpect(jsonPath("$.sessionId").value("sess-1"))
                .andExpect(jsonPath("$.description").value("do something"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getTask_returnsTask() throws Exception {
        when(taskManager.get("task-1")).thenReturn(Optional.of(testTask));

        mockMvc.perform(get("/api/v1/tasks/task-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-1"))
                .andExpect(jsonPath("$.description").value("do something"));
    }

    @Test
    void getTask_returns404ForUnknown() throws Exception {
        when(taskManager.get("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tasks/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTasks_returnsList() throws Exception {
        when(taskManager.listBySession("sess-1")).thenReturn(List.of(testTask));

        mockMvc.perform(get("/api/v1/tasks").param("sessionId", "sess-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-1"))
                .andExpect(jsonPath("$[0].sessionId").value("sess-1"));
    }

    @Test
    void updateStatus_returnsOk() throws Exception {
        mockMvc.perform(put("/api/v1/tasks/task-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RUNNING\"}"))
                .andExpect(status().isOk());
    }
}
