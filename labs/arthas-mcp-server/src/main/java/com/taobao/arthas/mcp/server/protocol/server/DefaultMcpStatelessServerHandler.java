/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

class DefaultMcpStatelessServerHandler implements McpStatelessServerHandler {

	private static final Logger logger = LoggerFactory.getLogger(DefaultMcpStatelessServerHandler.class);

	Map<String, McpStatelessRequestHandler<?>> requestHandlers;

	Map<String, McpStatelessNotificationHandler> notificationHandlers;

	private final CommandExecutor commandExecutor;

	private final ArthasCommandSessionManager commandSessionManager;

	public DefaultMcpStatelessServerHandler(Map<String, McpStatelessRequestHandler<?>> requestHandlers,
                                            Map<String, McpStatelessNotificationHandler> notificationHandlers,
                                            CommandExecutor commandExecutor) {
		this.requestHandlers = requestHandlers;
		this.notificationHandlers = notificationHandlers;
		this.commandExecutor = commandExecutor;
		this.commandSessionManager = new ArthasCommandSessionManager(commandExecutor);
	}

	@Override
	public CompletableFuture<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext ctx, McpSchema.JSONRPCRequest req) {
		// Create a temporary session for this request
		String tempSessionId = UUID.randomUUID().toString();
		ArthasCommandSessionManager.CommandSessionBinding binding = commandSessionManager.createCommandSession(tempSessionId);
		ArthasCommandContext commandContext = new ArthasCommandContext(commandExecutor, binding);

		McpStatelessRequestHandler<?> handler = requestHandlers.get(req.getMethod());
		if (handler == null) {
			// Clean up session if handler not found
			closeSession(binding);
			CompletableFuture<McpSchema.JSONRPCResponse> f = new CompletableFuture<>();
			f.completeExceptionally(new McpError("Missing handler for request type: " + req.getMethod()));
			return f;
		}
		try {
			@SuppressWarnings("unchecked")
			CompletableFuture<Object> result = (CompletableFuture<Object>) handler
					.handle(ctx, commandContext, req.getParams());
			return result.handle((r, ex) -> {
				// Clean up session after execution
				closeSession(binding);

				if (ex != null) {
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, req.getId(), null,
							new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, cause.getMessage(), null));
				}
				return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, req.getId(), r, null);
			});
		} catch (Throwable t) {
			// Clean up session on error
			closeSession(binding);

			CompletableFuture<McpSchema.JSONRPCResponse> f = new CompletableFuture<>();
			f.completeExceptionally(t);
			return f;
		}
	}

	private void closeSession(ArthasCommandSessionManager.CommandSessionBinding binding) {
		try {
			commandExecutor.closeSession(binding.getArthasSessionId());
		} catch (Exception e) {
			logger.warn("Failed to close temporary session: {}", binding.getArthasSessionId(), e);
		}
	}

	@Override
	public CompletableFuture<Void> handleNotification(McpTransportContext ctx,
													  McpSchema.JSONRPCNotification note) {
		McpStatelessNotificationHandler handler = notificationHandlers.get(note.getMethod());
		if (handler == null) {
			logger.warn("Missing handler for notification: {}", note.getMethod());
			return CompletableFuture.completedFuture(null);
		}
		try {
			return handler.handle(ctx, note.getParams());
		} catch (Throwable t) {
			CompletableFuture<Void> f = new CompletableFuture<>();
			f.completeExceptionally(t);
			return f;
		}
	}

}
