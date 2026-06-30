/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.JSONRPCResponse.JSONRPCError;
import com.taobao.arthas.mcp.server.util.Assert;

/**
 * Exception class for representing JSON-RPC errors in MCP protocol.
 *
 * @author Yeaury
 */
public class McpError extends RuntimeException {

    private JSONRPCError jsonRpcError;

    public McpError(JSONRPCError jsonRpcError) {
        super(jsonRpcError.getMessage());
        this.jsonRpcError = jsonRpcError;
    }

    @Deprecated
    public McpError(Object error) {
        super(error.toString());
    }

    public JSONRPCError getJsonRpcError() {
        return jsonRpcError;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        if (jsonRpcError != null) {
            builder.append("\n");
            builder.append(jsonRpcError.toString());
        }
        return builder.toString();
    }

    public static Builder builder(int errorCode) {
        return new Builder(errorCode);
    }

    public static class Builder {

        private final int code;

        private String message;

        private Object data;

        private Builder(int code) {
            this.code = code;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public McpError build() {
            Assert.hasText(message, "message must not be empty");
            return new McpError(new JSONRPCError(code, message, data));
        }

    }

    public static Throwable findRootCause(Throwable throwable) {
        Assert.notNull(throwable, "throwable must not be null");
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    public static String aggregateExceptionMessages(Throwable throwable) {
        Assert.notNull(throwable, "throwable must not be null");

        StringBuilder messages = new StringBuilder();
        Throwable current = throwable;

        while (current != null) {
            if (messages.length() > 0) {
                messages.append("\n  Caused by: ");
            }

            messages.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                messages.append(": ").append(current.getMessage());
            }

            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }

        return messages.toString();
    }

}
