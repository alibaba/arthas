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

import java.io.IOException;

/**
 * Java API for in-process profiling. Serves as a wrapper around
 * async-profiler native library. This class is a singleton.
 * The first call to {@link #getInstance()} initiates loading of
 * libasyncProfiler.so.
 */
public class AsyncProfiler implements AsyncProfilerMXBean {
    private static AsyncProfiler instance;

    private AsyncProfiler() {
    }

    public static AsyncProfiler getInstance() {
        return getInstance(null);
    }

    public static synchronized AsyncProfiler getInstance(String libPath) {
        if (instance != null) {
            return instance;
        }

        if (libPath == null) {
            System.loadLibrary("asyncProfiler");
        } else {
            System.load(libPath);
        }

        instance = new AsyncProfiler();
        return instance;
    }

    /**
     * Start profiling
     *
     * @param event Profiling event, see {@link Events}
     * @param interval Sampling interval, e.g. nanoseconds for Events.CPU
     * @throws IllegalStateException If profiler is already running
     */
    @Override
    public void start(String event, long interval) throws IllegalStateException {
        start0(event, interval, true);
    }

    /**
     * Start or resume profiling without resetting collected data.
     * Note that event and interval may change since the previous profiling session.
     *
     * @param event Profiling event, see {@link Events}
     * @param interval Sampling interval, e.g. nanoseconds for Events.CPU
     * @throws IllegalStateException If profiler is already running
     */
    @Override
    public void resume(String event, long interval) throws IllegalStateException {
        start0(event, interval, false);
    }

    /**
     * Stop profiling (without dumping results)
     *
     * @throws IllegalStateException If profiler is not running
     */
    @Override
    public void stop() throws IllegalStateException {
        stop0();
    }

    /**
     * Get the number of samples collected during the profiling session
     *
     * @return Number of samples
     */
    @Override
    public native long getSamples();

    /**
     * Get profiler agent version, e.g. "1.0"
     *
     * @return Version string
     */
    @Override
    public String getVersion() {
        try {
            return execute0("version");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Execute an agent-compatible profiling command -
     * the comma-separated list of arguments described in arguments.cpp
     *
     * @param command Profiling command
     * @return The command result
     * @throws IllegalArgumentException If failed to parse the command
     * @throws IOException If failed to create output file
     */
    @Override
    public String execute(String command) throws IllegalArgumentException, IOException {
        return execute0(command);
    }

    /**
     * Dump profile in 'collapsed stacktraces' format
     *
     * @param counter Which counter to display in the output
     * @return Textual representation of the profile
     */
    @Override
    public String dumpCollapsed(Counter counter) {
        try {
            return execute0("collapsed,counter=" + counter.name().toLowerCase());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Dump collected stack traces
     *
     * @param maxTraces Maximum number of stack traces to dump. 0 means no limit
     * @return Textual representation of the profile
     */
    @Override
    public String dumpTraces(int maxTraces) {
        try {
            return execute0("summary,traces=" + maxTraces);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Dump flat profile, i.e. the histogram of the hottest methods
     *
     * @param maxMethods Maximum number of methods to dump. 0 means no limit
     * @return Textual representation of the profile
     */
    @Override
    public String dumpFlat(int maxMethods) {
        try {
            return execute0("summary,flat=" + maxMethods);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Add the given thread to the set of profiled threads.
     * 'filter' option must be enabled to use this method.
     *
     * @param thread Thread to include in profiling
     */
    public void addThread(Thread thread) {
        filterThread(thread, true);
    }

    /**
     * Remove the given thread from the set of profiled threads.
     * 'filter' option must be enabled to use this method.
     *
     * @param thread Thread to exclude from profiling
     */
    public void removeThread(Thread thread) {
        filterThread(thread, false);
    }

    private void filterThread(Thread thread, boolean enable) {
        if (thread == null) {
            filterThread0(null, enable);
        } else {
            // Need to take lock to avoid race condition with a thread state change
            synchronized (thread) {
                Thread.State state = thread.getState();
                if (state != Thread.State.NEW && state != Thread.State.TERMINATED) {
                    filterThread0(thread, enable);
                }
            }
        }
    }

    private native void start0(String event, long interval, boolean reset) throws IllegalStateException;
    private native void stop0() throws IllegalStateException;
    private native String execute0(String command) throws IllegalArgumentException, IOException;
    private native void filterThread0(Thread thread, boolean enable);
}