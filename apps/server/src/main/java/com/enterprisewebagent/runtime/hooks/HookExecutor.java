package com.enterprisewebagent.runtime.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HookExecutor {

    private static final Logger log = LoggerFactory.getLogger(HookExecutor.class);
    private static final int TIMEOUT_SECONDS = 10;

    public HookResult execute(Hook hook, HookContext context) {
        if (!hook.enabled()) {
            return HookResult.success("Hook disabled: " + hook.id());
        }

        String command = hook.command();
        if (command == null || command.isBlank()) {
            return HookResult.failure("Hook has no command: " + hook.id());
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);

            pb.environment().put("HOOK_TYPE", context.type().name());
            pb.environment().put("SESSION_ID", context.sessionId() != null ? context.sessionId() : "");

            if (context.data() != null) {
                context.data().forEach((key, value) ->
                        pb.environment().put("HOOK_" + key.toUpperCase(), value != null ? value.toString() : ""));
            }

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Hook timed out: {}", hook.id());
                return HookResult.failure("Hook timed out after " + TIMEOUT_SECONDS + " seconds: " + hook.id());
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.warn("Hook failed: {} exitCode={}", hook.id(), exitCode);
                boolean abort = exitCode == 2; // exit code 2 signals abort
                return new HookResult(false, output.trim(), abort);
            }

            return HookResult.success(output.trim());
        } catch (IOException e) {
            log.error("Hook execution error: {}", hook.id(), e);
            return HookResult.failure("Error executing hook: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HookResult.failure("Hook interrupted: " + hook.id());
        }
    }
}
