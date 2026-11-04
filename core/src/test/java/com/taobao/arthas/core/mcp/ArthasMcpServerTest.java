package com.taobao.arthas.core.mcp;

import com.taobao.arthas.mcp.server.protocol.server.McpNettyServer;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpHttpRequestHandler;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArthasMcpServerTest {

    @Test
    public void stopShouldNotWaitForeverWhenUnifiedHandlerCloseNeverCompletes() throws Exception {
        ArthasMcpServer server = new ArthasMcpServer("/mcp", null, "STREAMABLE");
        McpHttpRequestHandler handler = mock(McpHttpRequestHandler.class);
        when(handler.closeGracefully()).thenReturn(new CompletableFuture<Void>());
        setField(server, "unifiedMcpHandler", handler);

        McpNettyServer streamableServer = mock(McpNettyServer.class);
        when(streamableServer.closeGracefully()).thenReturn(CompletableFuture.<Void>completedFuture(null));
        setField(server, "streamableServer", streamableServer);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> stopFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                server.stop();
            }
        });

        try {
            stopFuture.get(7000, TimeUnit.MILLISECONDS);
            verify(streamableServer).closeGracefully();
        } catch (TimeoutException e) {
            fail("stop should return after MCP graceful shutdown timeout");
        } finally {
            stopFuture.cancel(true);
            executor.shutdownNow();
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
