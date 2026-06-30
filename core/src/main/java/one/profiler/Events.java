/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

/**
 * Predefined event names to use in {@link AsyncProfiler#start(String, long)}
 */
public class Events {
    public static final String CPU    = "cpu";
    public static final String ALLOC  = "alloc";
    public static final String LOCK   = "lock";
    public static final String WALL   = "wall";
    public static final String CTIMER = "ctimer";
    public static final String ITIMER = "itimer";
}
