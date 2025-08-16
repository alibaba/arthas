package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.concurrent.CompletableFuture;

public interface McpStreamableServerTransportProvider extends McpServerTransportProvider {


    void setSessionFactory(McpStreamableServerSession.Factory sessionFactory);

    CompletableFuture<Void> notifyClients(String method, Object params);

    default void close() {
        this.closeGracefully();
    }

    String protocolVersion();

}
