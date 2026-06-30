/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.CompletableFuture;

/**
 * A placeholder implementation of McpLoggableSession that represents a missing or unavailable transport session.
 * This class is used when no active transport is available but session operations are attempted.
 */
public class MissingMcpTransportSession implements McpSession {

    private final String sessionId;

    public MissingMcpTransportSession(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(
                new IllegalStateException("Stream unavailable for session " + this.sessionId)
        );
        return future;
    }

    @Override
    public CompletableFuture<Void> sendNotification(String method, Object params) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(
                new IllegalStateException("Stream unavailable for session " + this.sessionId)
        );
        return future;
    }

    @Override
    public CompletableFuture<Void> closeGracefully() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        // Nothing to close for a missing session
    }


}
