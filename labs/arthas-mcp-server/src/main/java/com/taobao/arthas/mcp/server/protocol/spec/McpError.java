package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.JSONRPCResponse.JSONRPCError;

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

	public McpError(Object error) {
		super(error.toString());
	}

	public JSONRPCError getJsonRpcError() {
		return jsonRpcError;
	}

}
