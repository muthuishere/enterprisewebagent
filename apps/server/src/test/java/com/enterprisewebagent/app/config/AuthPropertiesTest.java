package com.enterprisewebagent.app.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthPropertiesTest {

    @Test
    void isValidKey_whenDisabled_alwaysReturnsTrue() {
        var props = new AuthProperties(false, List.of("secret-key"));
        assertTrue(props.isValidKey("wrong-key"));
        assertTrue(props.isValidKey(null));
        assertTrue(props.isValidKey(""));
    }

    @Test
    void isValidKey_whenEnabledWithEmptyList_alwaysReturnsTrue() {
        var props = new AuthProperties(true, List.of());
        assertTrue(props.isValidKey("anything"));
        assertTrue(props.isValidKey(null));
    }

    @Test
    void isValidKey_whenEnabledWithNullList_alwaysReturnsTrue() {
        var props = new AuthProperties(true, null);
        assertTrue(props.isValidKey("anything"));
    }

    @Test
    void isValidKey_whenEnabled_matchesValidKey() {
        var props = new AuthProperties(true, List.of("key-1", "key-2"));
        assertTrue(props.isValidKey("key-1"));
        assertTrue(props.isValidKey("key-2"));
    }

    @Test
    void isValidKey_whenEnabled_rejectsInvalidKey() {
        var props = new AuthProperties(true, List.of("key-1", "key-2"));
        assertFalse(props.isValidKey("wrong"));
        assertFalse(props.isValidKey(null));
        assertFalse(props.isValidKey(""));
    }

    @Test
    void isValidKey_whenEnabled_ignoresBlankKeysInList() {
        var props = new AuthProperties(true, List.of("", "  ", "valid-key"));
        assertTrue(props.isValidKey("valid-key"));
        assertFalse(props.isValidKey(""));
        assertFalse(props.isValidKey("  "));
    }
}
