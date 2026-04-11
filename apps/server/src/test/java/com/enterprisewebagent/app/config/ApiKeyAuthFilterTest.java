package com.enterprisewebagent.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthFilterTest {

    private final FilterChain filterChain = mock(FilterChain.class);

    @Test
    void authDisabled_allRequestsPassThrough() throws ServletException, IOException {
        var props = new AuthProperties(false, List.of("secret"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authEnabled_validBearerToken_passesThrough() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        request.addHeader("Authorization", "Bearer my-secret-key");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authEnabled_validXApiKeyHeader_passesThrough() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        request.addHeader("X-API-Key", "my-secret-key");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authEnabled_missingKey_returns401() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or missing API key"));
    }

    @Test
    void authEnabled_invalidKey_returns401() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        request.addHeader("Authorization", "Bearer wrong-key");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or missing API key"));
    }

    @Test
    void authEnabled_healthEndpoint_alwaysAccessible() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/config/health");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authEnabled_actuatorHealthEndpoint_alwaysAccessible() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authEnabled_bearerTokenPreferredOverXApiKey() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("bearer-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        request.addHeader("Authorization", "Bearer bearer-key");
        request.addHeader("X-API-Key", "wrong-key");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authEnabled_responseBodyIsJson() throws ServletException, IOException {
        var props = new AuthProperties(true, List.of("my-secret-key"));
        var filter = new ApiKeyAuthFilter(props);

        var request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("application/json", response.getContentType());
        assertEquals("{\"error\":\"Invalid or missing API key\"}", response.getContentAsString());
    }
}
