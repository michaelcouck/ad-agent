package com.ikube.marketingmcp.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketing.guard")
public record GuardProperties(
        boolean writeMode,
        boolean dryRun,
        boolean requireConfirmationToken,
        String confirmationToken,
        int maxDailyBudgetChangePercent,
        Path changeLogPath) {
}
