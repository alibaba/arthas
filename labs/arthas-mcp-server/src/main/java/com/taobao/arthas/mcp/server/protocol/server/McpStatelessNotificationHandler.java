package com.taobao.arthas.mcp.server.protocol.server;


import java.util.concurrent.CompletableFuture;

/**
 * Handler for MCP notifications in a stateless server.
 */
public interface McpStatelessNotificationHandler {

	/**
	 * Handle to notification and complete once done.
	 * @param transportContext {@link McpTransportContext} associated with the transport
	 * @param params the payload of the MCP notification
	 * @return Mono which completes once the processing is done
	 */
	CompletableFuture<Void> handle(McpTransportContext transportContext, Object params);

}
