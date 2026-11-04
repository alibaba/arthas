package com.taobao.arthas.mcp.server.protocol.server.store;

import com.taobao.arthas.mcp.server.protocol.spec.EventStore;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory implementation of EventStore.
 *
 * @author Yeaury
 */
public class InMemoryEventStore implements EventStore {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);
    
    /** Global event ID counter */
    private final AtomicLong globalEventIdCounter = new AtomicLong(0);
    
    /**
     * Events storage: sessionId -> list of events
     */
    private final Map<String, List<StoredEvent>> sessionEvents = new ConcurrentHashMap<>();
    
    /**
     * Event ID to session mapping for fast lookup
     */
    private final Map<String, String> eventIdToSession = new ConcurrentHashMap<>();
    
    /**
     * Maximum events to keep per session (prevent memory leaks)
     */
    private final int maxEventsPerSession;
    
    /**
     * Default retention time in milliseconds (24 hours)
     */
    private final long defaultRetentionMs;
    
    public InMemoryEventStore() {
        this(1000, 24 * 60 * 60 * 1000L); // 1000 events, 24 hours
    }
    
    public InMemoryEventStore(int maxEventsPerSession, long defaultRetentionMs) {
        this.maxEventsPerSession = maxEventsPerSession;
        this.defaultRetentionMs = defaultRetentionMs;
    }
    
    @Override
    public String storeEvent(String sessionId, McpSchema.JSONRPCMessage message) {
        String eventId = String.valueOf(globalEventIdCounter.incrementAndGet());
        Instant timestamp = Instant.now();
        
        StoredEvent event = new StoredEvent(eventId, sessionId, message, timestamp);
        
        sessionEvents.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(event);
        eventIdToSession.put(eventId, sessionId);
        
        // Cleanup old events if needed
        List<StoredEvent> events = sessionEvents.get(sessionId);
        if (events.size() > maxEventsPerSession) {
            // Remove oldest events
            int toRemove = events.size() - maxEventsPerSession;
            for (int i = 0; i < toRemove; i++) {
                StoredEvent removedEvent = events.remove(0);
                eventIdToSession.remove(removedEvent.getEventId());
            }
            logger.debug("Cleaned up {} old events for session {}", toRemove, sessionId);
        }
        
        logger.trace("Stored event {} for session {}", eventId, sessionId);
        return eventId;
    }
    
    @Override
    public Stream<StoredEvent> getEventsForSession(String sessionId, String fromEventId) {
        List<StoredEvent> events = sessionEvents.get(sessionId);
        if (events == null || events.isEmpty()) {
            return Stream.empty();
        }
        
        if (fromEventId == null) {
            return Stream.empty();
        }
        
        boolean foundStartEvent = false;
        List<StoredEvent> result = new ArrayList<>();
        
        for (StoredEvent event : events) {
            if (!foundStartEvent) {
                if (event.getEventId().equals(fromEventId)) {
                    foundStartEvent = true;
                    result.add(event);
                    // clear the replayed events
                    events.remove(event);
                    eventIdToSession.remove(event.getEventId());
                }
                continue;
            }
            result.add(event);
        }
        
        return result.stream();
    }

    @Override
    public void cleanupOldEvents(String sessionId, long maxAge) {
        List<StoredEvent> events = sessionEvents.get(sessionId);
        if (events == null || events.isEmpty()) {
            return;
        }
        
        Instant cutoff = Instant.now().minusMillis(maxAge);
        
        List<StoredEvent> toRemove = events.stream()
            .filter(event -> event.getTimestamp().isBefore(cutoff))
            .collect(Collectors.toList());
        
        for (StoredEvent event : toRemove) {
            events.remove(event);
            eventIdToSession.remove(event.getEventId());
        }
        
        if (!toRemove.isEmpty()) {
            logger.debug("Cleaned up {} old events for session {}", toRemove.size(), sessionId);
        }
    }
    
    @Override
    public void removeSessionEvents(String sessionId) {
        List<StoredEvent> events = sessionEvents.remove(sessionId);
        if (events != null) {
            for (StoredEvent event : events) {
                eventIdToSession.remove(event.getEventId());
            }
            logger.debug("Removed {} events for session {}", events.size(), sessionId);
        }
    }

    public void cleanupExpiredEvents() {
        for (String sessionId : sessionEvents.keySet()) {
            cleanupOldEvents(sessionId, defaultRetentionMs);
        }
    }
}
