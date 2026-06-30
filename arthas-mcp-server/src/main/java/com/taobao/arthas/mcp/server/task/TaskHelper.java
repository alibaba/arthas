/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.TaskStatus;

/**
 * Utility methods for task status checks.
 *
 * @author Yeaury
 */
public final class TaskHelper {

    private TaskHelper() {}

    /** Returns true if the status is COMPLETED, FAILED, or CANCELLED. */
    public static boolean isTerminal(TaskStatus status) {
        if (status == null) {
            return false;
        }
        return status == TaskStatus.COMPLETED
            || status == TaskStatus.FAILED
            || status == TaskStatus.CANCELLED;
    }

    /**
     * Returns true if the transition from {@code from} to {@code to} is valid.
     *
     * <p>Terminal states cannot transition further. WORKING can transition to any state.
     * INPUT_REQUIRED can transition to WORKING or any terminal state.
     */
    public static boolean isValidTransition(TaskStatus from, TaskStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (isTerminal(from)) {
            return false;
        }
        if (from == TaskStatus.WORKING) {
            return true;
        }
        if (from == TaskStatus.INPUT_REQUIRED) {
            return to == TaskStatus.WORKING || isTerminal(to);
        }
        return false;
    }
}
