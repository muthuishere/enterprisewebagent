package com.enterprisewebagent.runtime.hooks;

public record HookResult(boolean success, String output, boolean abort) {

    public static HookResult success(String output) {
        return new HookResult(true, output, false);
    }

    public static HookResult failure(String output) {
        return new HookResult(false, output, false);
    }

    public static HookResult aborted(String output) {
        return new HookResult(false, output, true);
    }
}
