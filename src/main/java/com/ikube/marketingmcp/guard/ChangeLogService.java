package com.ikube.marketingmcp.guard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikube.marketingmcp.config.GuardProperties;
import org.springframework.stereotype.Component;

@Component
public class ChangeLogService {

    private final GuardProperties properties;
    private final ObjectMapper objectMapper;

    public ChangeLogService(GuardProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void log(String action, GuardDecision decision, Map<String, Object> payload) {
        try {
            if (properties.changeLogPath().getParent() != null) {
                Files.createDirectories(properties.changeLogPath().getParent());
            }
            Map<String, Object> entry = Map.of(
                    "timestamp", OffsetDateTime.now().toString(),
                    "action", action,
                    "decision", decision,
                    "payload", payload);
            Files.writeString(properties.changeLogPath(), objectMapper.writeValueAsString(entry) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    Files.exists(properties.changeLogPath())
                            ? java.nio.file.StandardOpenOption.APPEND
                            : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write change log", e);
        }
    }
}
