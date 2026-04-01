/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

/**
 * Message types for side-channel communication during task execution.
 *
 * <p>Request and Notification are dequeued for delivery to the client.
 * Response messages are retrieved exclusively via {@code waitForResponse}.
 *
 * @author Yeaury
 */
public abstract class QueuedMessage {

    /** Server-to-client request (e.g. elicitation, sampling) requiring a response. */
    public static class Request extends QueuedMessage {
        private final Object requestId;
        private final String method;
        private final McpSchema.Request request;

        public Request(Object requestId, String method, McpSchema.Request request) {
            this.requestId = requestId;
            this.method = method;
            this.request = request;
        }

        public Object requestId() {
            return requestId;
        }

        public String method() {
            return method;
        }

        public McpSchema.Request request() {
            return request;
        }

        @Override
        public String toString() {
            return "QueuedMessage.Request{requestId=" + requestId + ", method='" + method + "'}";
        }
    }

    /** Client response to a prior Request. */
    public static class Response extends QueuedMessage {
        private final Object requestId;
        private final McpSchema.Result result;

        public Response(Object requestId, McpSchema.Result result) {
            this.requestId = requestId;
            this.result = result;
        }

        public Object requestId() {
            return requestId;
        }

        public McpSchema.Result result() {
            return result;
        }

        @Override
        public String toString() {
            return "QueuedMessage.Response{requestId=" + requestId + "}";
        }
    }

    /** Async notification (e.g. progress update) that requires no response. */
    public static class Notification extends QueuedMessage {
        private final String method;
        private final Object notification;

        public Notification(String method, Object notification) {
            this.method = method;
            this.notification = notification;
        }

        public String method() {
            return method;
        }

        public Object notification() {
            return notification;
        }

        @Override
        public String toString() {
            return "QueuedMessage.Notification{method='" + method + "'}";
        }
    }
}
