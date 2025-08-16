/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

/**
 * Interface for extracting transport context from server requests.
 * This allows inspection of HTTP transport level metadata during request processing.
 *
 * @param <T> the type of server request
 */
@FunctionalInterface
public interface McpTransportContextExtractor<T> {

    /**
     * Extract transport context from the server request.
     * 
     * @param serverRequest the server request to extract context from
     * @param context the base context to fill in
     * @return the updated context with extracted information
     */
    McpTransportContext extract(T serverRequest, McpTransportContext context);

}