package com.ikube.marketingmcp.model;

public record ToolResult(boolean ok, String message, Object data) {

    public static ToolResult ok(Object data) {
        return new ToolResult(true, "ok", data);
    }

    public static ToolResult ok(String message, Object data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult blocked(String message, Object data) {
        return new ToolResult(false, message, data);
    }
}
