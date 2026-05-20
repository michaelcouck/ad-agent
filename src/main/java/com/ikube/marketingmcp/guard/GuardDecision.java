package com.ikube.marketingmcp.guard;

public record GuardDecision(String action, boolean allowed, boolean dryRun, String reason) {

    public static GuardDecision allowed(String action) {
        return new GuardDecision(action, true, false, "allowed");
    }

    public static GuardDecision dryRun(String action) {
        return new GuardDecision(action, false, true, "dry-run");
    }

    public static GuardDecision blocked(String action, String reason) {
        return new GuardDecision(action, false, false, reason);
    }
}
