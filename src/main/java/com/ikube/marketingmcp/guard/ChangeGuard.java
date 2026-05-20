package com.ikube.marketingmcp.guard;

import com.ikube.marketingmcp.config.GuardProperties;
import org.springframework.stereotype.Component;

@Component
public class ChangeGuard {

    private final GuardProperties properties;

    public ChangeGuard(GuardProperties properties) {
        this.properties = properties;
    }

    public GuardDecision checkWrite(String action, String confirmationToken) {
        if (!properties.writeMode()) {
            return GuardDecision.blocked(action, "Write mode is disabled. Set MARKETING_MCP_WRITE_MODE=true.");
        }
        if (properties.requireConfirmationToken()) {
            if (blank(properties.confirmationToken())) {
                return GuardDecision.blocked(action, "Confirmation token is required but MARKETING_MCP_CONFIRMATION_TOKEN is empty.");
            }
            if (!properties.confirmationToken().equals(confirmationToken)) {
                return GuardDecision.blocked(action, "Confirmation token did not match.");
            }
        }
        if (properties.dryRun()) {
            return GuardDecision.dryRun(action);
        }
        return GuardDecision.allowed(action);
    }

    public GuardDecision checkBudgetChange(long currentMicros, long proposedMicros, String confirmationToken) {
        GuardDecision writeDecision = checkWrite("update_campaign_budget", confirmationToken);
        if (!writeDecision.allowed()) {
            return writeDecision;
        }
        if (currentMicros <= 0) {
            return writeDecision;
        }
        long delta = Math.abs(proposedMicros - currentMicros);
        double percent = (delta * 100.0d) / currentMicros;
        if (percent > properties.maxDailyBudgetChangePercent()) {
            return GuardDecision.blocked("update_campaign_budget",
                    "Budget change is " + Math.round(percent) + "%, above limit "
                            + properties.maxDailyBudgetChangePercent() + "%.");
        }
        return writeDecision;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
