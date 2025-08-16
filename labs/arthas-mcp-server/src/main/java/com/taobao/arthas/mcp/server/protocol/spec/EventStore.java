package com.taobao.arthas.mcp.server.protocol.spec;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * EventStore interface for storing and replaying JSON-RPC events.
 * Supports event persistence and stream resumability for MCP Streamable HTTP protocol.
 *
 * @author Yeaury
 */
public interface EventStore {

    class StoredEvent {
        private final String eventId;
        private final String sessionId;
        private final McpSchema.JSONRPCMessage message;
        private final Instant timestamp;
        
        public StoredEvent(String eventId, String sessionId, McpSchema.JSONRPCMessage message, Instant timestamp) {
            this.eventId = eventId;
            this.sessionId = sessionId;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getEventId() {
            return eventId;
        }
        public String getSessionId() {
            return sessionId;
        }
        public McpSchema.JSONRPCMessage getMessage() {
            return message;
        }
        public Instant getTimestamp() {
            return timestamp;
        }
    }

    String storeEvent(String sessionId, McpSchema.JSONRPCMessage message);

    Stream<StoredEvent> getEventsForSession(String sessionId, String fromEventId);

    void cleanupOldEvents(String sessionId, long maxAge);

    void removeSessionEvents(String sessionId);

}
