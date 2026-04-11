package com.enterprisewebagent.app.api;

import com.enterprisewebagent.runtime.commands.CommandDispatcher;
import com.enterprisewebagent.runtime.commands.CommandRegistry;
import com.enterprisewebagent.runtime.commands.builtin.BuiltInCommandRegistrar;
import com.enterprisewebagent.runtime.planning.PlanManager;
import com.enterprisewebagent.runtime.prompt.PromptAssembler;
import com.enterprisewebagent.runtime.prompt.PromptSection;
import com.enterprisewebagent.runtime.provider.DefaultModelProviderRegistry;
import com.enterprisewebagent.runtime.provider.StubModelProvider;
import com.enterprisewebagent.runtime.query.TurnEngine;
import com.enterprisewebagent.runtime.query.TurnResult;
import com.enterprisewebagent.runtime.session.InMemorySessionManager;
import com.enterprisewebagent.runtime.session.Session;
import com.enterprisewebagent.runtime.session.SessionManager;
import com.enterprisewebagent.runtime.session.SessionStatus;
import com.enterprisewebagent.runtime.tasks.TaskManager;
import com.enterprisewebagent.runtime.tools.ToolDefinition;
import com.enterprisewebagent.runtime.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionManager sessionManager;

    @MockitoBean
    private TurnEngine turnEngine;

    @MockitoBean
    private PromptAssembler promptAssembler;

    @MockitoBean
    private ToolRegistry toolRegistry;

    @MockitoBean
    private PlanManager planManager;

    @MockitoBean
    private TaskManager taskManager;

    @MockitoBean
    private DefaultModelProviderRegistry modelProviderRegistry;

    @MockitoBean
    private CommandDispatcher commandDispatcher;

    private Session testSession;

    @BeforeEach
    void setUp() {
        testSession = new Session("sess-1", "ws-1", Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"), SessionStatus.ACTIVE, List.of());
    }

    @Test
    void createSession_returnsSession() throws Exception {
        when(sessionManager.create("default")).thenReturn(testSession);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\":\"default\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sess-1"))
                .andExpect(jsonPath("$.workspaceId").value("ws-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getSession_returnsSession() throws Exception {
        when(sessionManager.get("sess-1")).thenReturn(Optional.of(testSession));

        mockMvc.perform(get("/api/v1/sessions/sess-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sess-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getSession_returns404ForUnknown() throws Exception {
        when(sessionManager.get("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sessions/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeTurn_returnsResult() throws Exception {
        when(sessionManager.get("sess-1")).thenReturn(Optional.of(testSession));
        when(promptAssembler.assemble(any())).thenReturn(List.of(new PromptSection("test", "content", false)));
        when(toolRegistry.resolveTools(any())).thenReturn(List.of());
        when(turnEngine.executeTurn(any())).thenReturn(
                new TurnResult("sess-1", "Hello!", List.of(), true));

        mockMvc.perform(post("/api/v1/sessions/sess-1/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-1"))
                .andExpect(jsonPath("$.output").value("Hello!"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.toolCalls").value(0));
    }

    @Test
    void executeTurn_returns404ForUnknownSession() throws Exception {
        when(sessionManager.get("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/sessions/unknown/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"Hi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeTurn_returns400ForMissingInput() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/sess-1/turns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("input is required"));
    }

    @Test
    void closeSession_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/sess-1/close"))
                .andExpect(status().isOk());

        verify(sessionManager).close("sess-1");
    }
}
