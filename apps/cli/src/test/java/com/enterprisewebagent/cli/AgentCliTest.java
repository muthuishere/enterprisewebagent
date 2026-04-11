package com.enterprisewebagent.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;

class AgentCliTest {

    @Test
    void testHelpOutput() {
        var cli = new AgentCli();
        var cmd = new CommandLine(cli);
        var exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    void testVersionOutput() {
        var cli = new AgentCli();
        var cmd = new CommandLine(cli);
        var exitCode = cmd.execute("--version");
        assertEquals(0, exitCode);
    }

    @Test
    void testSessionSubcommandRegistered() {
        var cmd = new CommandLine(new AgentCli());
        assertNotNull(cmd.getSubcommands().get("session"), "session subcommand should be registered");
    }

    @Test
    void testTaskSubcommandRegistered() {
        var cmd = new CommandLine(new AgentCli());
        assertNotNull(cmd.getSubcommands().get("task"), "task subcommand should be registered");
    }

    @Test
    void testConfigSubcommandRegistered() {
        var cmd = new CommandLine(new AgentCli());
        assertNotNull(cmd.getSubcommands().get("config"), "config subcommand should be registered");
    }

    @Test
    void testHealthSubcommandRegistered() {
        var cmd = new CommandLine(new AgentCli());
        assertNotNull(cmd.getSubcommands().get("health"), "health subcommand should be registered");
    }

    @Test
    void testSessionSubcommandsRegistered() {
        var cmd = new CommandLine(new AgentCli());
        var sessionCmd = cmd.getSubcommands().get("session");
        assertNotNull(sessionCmd.getSubcommands().get("list"), "session list should be registered");
        assertNotNull(sessionCmd.getSubcommands().get("create"), "session create should be registered");
        assertNotNull(sessionCmd.getSubcommands().get("chat"), "session chat should be registered");
        assertNotNull(sessionCmd.getSubcommands().get("close"), "session close should be registered");
    }

    @Test
    void testTaskSubcommandsRegistered() {
        var cmd = new CommandLine(new AgentCli());
        var taskCmd = cmd.getSubcommands().get("task");
        assertNotNull(taskCmd.getSubcommands().get("list"), "task list should be registered");
        assertNotNull(taskCmd.getSubcommands().get("create"), "task create should be registered");
        assertNotNull(taskCmd.getSubcommands().get("status"), "task status should be registered");
    }

    @Test
    void testDefaultServerUrl() {
        var cli = new AgentCli();
        new CommandLine(cli).parseArgs();
        assertEquals("http://localhost:8080", cli.serverUrl);
    }

    @Test
    void testCustomServerUrl() {
        var cli = new AgentCli();
        new CommandLine(cli).parseArgs("--server", "http://myserver:9090");
        assertEquals("http://myserver:9090", cli.serverUrl);
    }
}
