package com.enterprisewebagent.runtime.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegistry {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(Command cmd) {
        commands.put(cmd.name(), cmd);
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    public List<Command> listAll() {
        return commands.values().stream()
                .sorted(Comparator.comparing(Command::name))
                .toList();
    }

    public int size() {
        return commands.size();
    }
}
