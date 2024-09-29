/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

/**
 * AsyncProfiler interface for JMX server.
 * How to register AsyncProfiler MBean:
 *
 * <pre>{@code
 *     ManagementFactory.getPlatformMBeanServer().registerMBean(
 *             AsyncProfiler.getInstance(),
 *             new ObjectName("one.profiler:type=AsyncProfiler")
 *     );
 * }</pre>
 */
public interface AsyncProfilerMXBean {
    void start(String event, long interval) throws IllegalStateException;
    void resume(String event, long interval) throws IllegalStateException;
    void stop() throws IllegalStateException;

    long getSamples();
    String getVersion();

    String execute(String command) throws IllegalArgumentException, IllegalStateException, java.io.IOException;

    String dumpCollapsed(Counter counter);
    String dumpTraces(int maxTraces);
    String dumpFlat(int maxMethods);
}