package com.taobao.arthas.mcp.server.protocol.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.spec.McpServerSession;
import com.taobao.arthas.mcp.server.protocol.spec.McpServerTransportProvider;
import com.taobao.arthas.mcp.server.util.Assert;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP MCP server transport provider implementation based on Netty,
 * supporting server-to-client communication via SSE.
 *
 * @author Yeaury
 */
public class HttpNettyServerTransportProvider implements McpServerTransportProvider {
	public static final String DEFAULT_SSE_ENDPOINT = "/mcp";
	public static final String DEFAULT_MESSAGE_ENDPOINT = "/mcp/message";
	private final McpRequestHandler mcpRequestHandler;

	/**
	 * Create a new HttpServerTransportProvider instance.
	 * @param messageEndpoint Message endpoint path, e.g. "/mcp/message"
	 * @param sseEndpoint SSE endpoint path, e.g. "/mcp"
	 * @param objectMapper Object mapper for JSON serialization/deserialization
	 */
	public HttpNettyServerTransportProvider(String messageEndpoint, String sseEndpoint,
											ObjectMapper objectMapper) {
		Assert.hasText(messageEndpoint, "Message endpoint path cannot be empty");
		Assert.hasText(sseEndpoint, "SSE endpoint path cannot be empty");
		Assert.notNull(objectMapper, "Object mapper cannot be null");

		this.mcpRequestHandler = new McpRequestHandler(messageEndpoint, sseEndpoint, objectMapper);
	}

	@Override
	public void setSessionFactory(McpServerSession.Factory sessionFactory) {
		this.mcpRequestHandler.setSessionFactory(sessionFactory);
	}

	@Override
	public CompletableFuture<Void> notifyClients(String method, Object params) {
		return mcpRequestHandler.notifyClients(method, params);
	}

	@Override
	public CompletableFuture<Void> closeGracefully() {
		return mcpRequestHandler.closeGracefully();
	}

	@Override
	public McpRequestHandler getMcpRequestHandler() {
		return mcpRequestHandler;
	}

	public static class Builder {

		private String messageEndpoint = DEFAULT_MESSAGE_ENDPOINT;

		private String sseEndpoint = DEFAULT_SSE_ENDPOINT;

		private ObjectMapper objectMapper = new ObjectMapper();

		public Builder messageEndpoint(String messageEndpoint) {
			this.messageEndpoint = messageEndpoint;
			return this;
		}

		public Builder sseEndpoint(String sseEndpoint) {
			this.sseEndpoint = sseEndpoint;
			return this;
		}

		public Builder objectMapper(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
			return this;
		}

		public HttpNettyServerTransportProvider build() {
			return new HttpNettyServerTransportProvider(messageEndpoint, sseEndpoint, objectMapper);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

}