package com.enterprisewebagent.runtime.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrontmatterParserTest {

    @Test
    void parseValidFrontmatter() {
        String markdown = """
                ---
                name: my-skill
                description: Does something useful
                version: 1.0
                ---
                # Skill content here
                Some body text.
                """;
        var result = FrontmatterParser.parse(markdown);
        assertEquals("my-skill", result.frontmatter().get("name"));
        assertEquals("Does something useful", result.frontmatter().get("description"));
        assertEquals("1.0", result.frontmatter().get("version"));
        assertEquals(3, result.frontmatter().size());
        assertTrue(result.content().contains("# Skill content here"));
        assertTrue(result.content().contains("Some body text."));
    }

    @Test
    void parseContentAfterFrontmatter() {
        String markdown = """
                ---
                name: test
                ---
                First line of content.
                Second line of content.
                """;
        var result = FrontmatterParser.parse(markdown);
        assertEquals("test", result.frontmatter().get("name"));
        assertTrue(result.content().startsWith("First line of content."));
        assertTrue(result.content().contains("Second line of content."));
    }

    @Test
    void parseNoFrontmatter() {
        String markdown = "# Just a heading\nSome content.\n";
        var result = FrontmatterParser.parse(markdown);
        assertTrue(result.frontmatter().isEmpty());
        assertEquals(markdown, result.content());
    }

    @Test
    void parseFrontmatterWithComments() {
        String markdown = """
                ---
                name: skill-with-comments
                # this is a comment
                description: A skill
                ---
                Body.
                """;
        var result = FrontmatterParser.parse(markdown);
        assertEquals(2, result.frontmatter().size());
        assertEquals("skill-with-comments", result.frontmatter().get("name"));
        assertEquals("A skill", result.frontmatter().get("description"));
        assertNull(result.frontmatter().get("# this is a comment"));
    }

    @Test
    void parseValuesAreTrimmed() {
        String markdown = """
                ---
                name:   padded-value  \s
                description:  lots of spaces  \s
                ---
                Content.
                """;
        var result = FrontmatterParser.parse(markdown);
        assertEquals("padded-value", result.frontmatter().get("name"));
        assertEquals("lots of spaces", result.frontmatter().get("description"));
    }

    @Test
    void parseEmptyFrontmatter() {
        String markdown = """
                ---
                ---
                Content after empty frontmatter.
                """;
        var result = FrontmatterParser.parse(markdown);
        assertTrue(result.frontmatter().isEmpty());
        assertTrue(result.content().contains("Content after empty frontmatter."));
    }

    @Test
    void parseNullInput() {
        var result = FrontmatterParser.parse(null);
        assertTrue(result.frontmatter().isEmpty());
        assertEquals("", result.content());
    }

    @Test
    void parseEmptyInput() {
        var result = FrontmatterParser.parse("");
        assertTrue(result.frontmatter().isEmpty());
        assertEquals("", result.content());
    }

    @Test
    void parseNoClosingDelimiter() {
        String markdown = """
                ---
                name: incomplete
                No closing delimiter here.
                """;
        var result = FrontmatterParser.parse(markdown);
        assertTrue(result.frontmatter().isEmpty());
        assertEquals(markdown, result.content());
    }
}
