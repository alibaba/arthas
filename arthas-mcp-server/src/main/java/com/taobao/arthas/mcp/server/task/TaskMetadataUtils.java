/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for adding {@code relatedTask} metadata to notifications and results.
 *
 * @author Yeaury
 */
public final class TaskMetadataUtils {

    private TaskMetadataUtils() {}

    /**
     * Injects {@code _meta.relatedTask.taskId} into a notification.
     * {@link McpSchema.TaskStatusNotification} is returned unchanged (already contains taskId).
     */
    @SuppressWarnings("unchecked")
    public static Object addRelatedTaskMetadata(String taskId, Object notification) {
        if (notification == null || taskId == null) {
            return notification;
        }
        if (notification instanceof McpSchema.TaskStatusNotification) {
            return notification;
        }
        if (notification instanceof Map) {
            Map<String, Object> notifMap = new HashMap<>((Map<String, Object>) notification);
            Map<String, Object> meta = notifMap.containsKey("_meta") && notifMap.get("_meta") instanceof Map
                    ? new HashMap<>((Map<String, Object>) notifMap.get("_meta"))
                    : new HashMap<>();
            Map<String, Object> relatedTask = new HashMap<>();
            relatedTask.put("taskId", taskId);
            meta.put(McpSchema.RELATED_TASK_META_KEY, relatedTask);
            notifMap.put("_meta", meta);
            return notifMap;
        }
        return notification;
    }

    /** Merges {@code relatedTask: {taskId}} into a new metadata map, overlaying existing entries. */
    public static Map<String, Object> mergeRelatedTaskMetadata(String taskId, Map<String, Object> existingMeta) {
        Map<String, Object> taskIdMap = new HashMap<>();
        taskIdMap.put("taskId", taskId);
        return mergeRelatedTaskMetadata((Object) taskIdMap, existingMeta);
    }

    /** Merges {@code relatedTask: relatedTaskValue} into a new metadata map, overlaying existing entries. */
    public static Map<String, Object> mergeRelatedTaskMetadata(Object relatedTaskValue,
                                                                 Map<String, Object> existingMeta) {
        Map<String, Object> newMeta = new HashMap<>();
        newMeta.put(McpSchema.RELATED_TASK_META_KEY, relatedTaskValue);
        if (existingMeta != null) {
            newMeta.putAll(existingMeta);
        }
        return newMeta;
    }
}
