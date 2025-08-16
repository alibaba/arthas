package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

class DefaultMcpStatelessServerHandler implements McpStatelessServerHandler {

	private static final Logger logger = LoggerFactory.getLogger(DefaultMcpStatelessServerHandler.class);

	Map<String, McpStatelessRequestHandler<?>> requestHandlers;

	Map<String, McpStatelessNotificationHandler> notificationHandlers;

	private final CommandExecutor commandExecutor;

	public DefaultMcpStatelessServerHandler(Map<String, McpStatelessRequestHandler<?>> requestHandlers,
                                            Map<String, McpStatelessNotificationHandler> notificationHandlers,
                                            CommandExecutor commandExecutor) {
		this.requestHandlers = requestHandlers;
		this.notificationHandlers = notificationHandlers;
		this.commandExecutor = commandExecutor;
	}

	@Override
	public CompletableFuture<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext ctx, McpSchema.JSONRPCRequest req) {
		ArthasCommandContext commandContext = createCommandContext();
		McpStatelessRequestHandler<?> handler = requestHandlers.get(req.getMethod());
		if (handler == null) {
			CompletableFuture<McpSchema.JSONRPCResponse> f = new CompletableFuture<>();
			f.completeExceptionally(new McpError("Missing handler for request type: " + req.getMethod()));
			return f;
		}
		try {
			@SuppressWarnings("unchecked")
			CompletableFuture<Object> result = (CompletableFuture<Object>) handler
					.handle(ctx, commandContext, req.getParams());
			return result.handle((r, ex) -> {
				if (ex != null) {
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, req.getId(), null,
							new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, cause.getMessage(), null));
				}
				return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, req.getId(), r, null);
			});
		} catch (Throwable t) {
			CompletableFuture<McpSchema.JSONRPCResponse> f = new CompletableFuture<>();
			f.completeExceptionally(t);
			return f;
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

	private ArthasCommandContext createCommandContext() {
		return new ArthasCommandContext(commandExecutor);
	}

}
