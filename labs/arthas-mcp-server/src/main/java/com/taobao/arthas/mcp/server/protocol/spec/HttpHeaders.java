package com.taobao.arthas.mcp.server.protocol.spec;

public interface HttpHeaders {

	/**
	 * Identifies individual MCP sessions.
	 */
	String MCP_SESSION_ID = "mcp-session-id";

	/**
	 * Identifies events within an SSE Stream.
	 */
	String LAST_EVENT_ID = "last-event-id";

	/**
	 * Identifies the MCP protocol version.
	 */
	String PROTOCOL_VERSION = "MCP-Protocol-Version";

}
