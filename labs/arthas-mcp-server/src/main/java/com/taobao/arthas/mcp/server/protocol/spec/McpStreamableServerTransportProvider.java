/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface McpStreamableServerTransportProvider extends McpServerTransportProvider {


    void setSessionFactory(McpStreamableServerSession.Factory sessionFactory);

    CompletableFuture<Void> notifyClients(String method, Object params);

    default void close() {
        this.closeGracefully();
    }

    default List<String> protocolVersions() {
        return Arrays.asList(ProtocolVersions.MCP_2024_11_05);
    }

}
