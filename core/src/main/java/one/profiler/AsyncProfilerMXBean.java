/*
 * Copyright 2018 Andrei Pangin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
