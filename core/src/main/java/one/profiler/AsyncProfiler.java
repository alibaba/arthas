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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

        AsyncProfiler profiler = new AsyncProfiler();
        if (libPath != null) {
            System.load(libPath);
        } else {
            try {
                // No need to load library, if it has been preloaded with -agentpath
                profiler.getVersion();
            } catch (UnsatisfiedLinkError e) {
                File file = extractEmbeddedLib();
                if (file != null) {
                    try {
                        System.load(file.getPath());
                    } finally {
                        file.delete();
                    }
                } else {
                    System.loadLibrary("asyncProfiler");
                }
            }
        }

        instance = profiler;
        return profiler;
    }

    private static File extractEmbeddedLib() {
        String resourceName = "/" + getPlatformTag() + "/libasyncProfiler.so";
        InputStream in = AsyncProfiler.class.getResourceAsStream(resourceName);
        if (in == null) {
            return null;
        }

        try {
            String extractPath = System.getProperty("one.profiler.extractPath");
            File file = File.createTempFile("libasyncProfiler-", ".so",
                    extractPath == null || extractPath.isEmpty() ? null : new File(extractPath));
            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[32000];
                for (int bytes; (bytes = in.read(buf)) >= 0; ) {
                    out.write(buf, 0, bytes);
                }
            }
            return file;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static String getPlatformTag() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (os.contains("linux")) {
            if (arch.equals("amd64") || arch.equals("x86_64") || arch.contains("x64")) {
                return "linux-x64";
            } else if (arch.equals("aarch64") || arch.contains("arm64")) {
                return "linux-arm64";
            } else if (arch.equals("aarch32") || arch.contains("arm")) {
                return "linux-arm32";
            } else if (arch.contains("86")) {
                return "linux-x86";
            } else if (arch.contains("ppc64")) {
                return "linux-ppc64le";
            }
        } else if (os.contains("mac")) {
            return "macos";
        }
        throw new UnsupportedOperationException("Unsupported platform: " + os + "-" + arch);
    }

    /**
     * Start profiling
     *
     * @param event    Profiling event, see {@link Events}
     * @param interval Sampling interval, e.g. nanoseconds for Events.CPU
     * @throws IllegalStateException If profiler is already running
     */
    @Override
    public void start(String event, long interval) throws IllegalStateException {
        if (event == null) {
            throw new NullPointerException();
        }
        start0(event, interval, true);
    }

    /**
     * Start or resume profiling without resetting collected data.
     * Note that event and interval may change since the previous profiling session.
     *
     * @param event    Profiling event, see {@link Events}
     * @param interval Sampling interval, e.g. nanoseconds for Events.CPU
     * @throws IllegalStateException If profiler is already running
     */
    @Override
    public void resume(String event, long interval) throws IllegalStateException {
        if (event == null) {
            throw new NullPointerException();
        }
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
     * @throws IOException              If failed to create output file
     */
    @Override
    public String execute(String command) throws IllegalArgumentException, IllegalStateException, IOException {
        if (command == null) {
            throw new NullPointerException();
        }
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
            return execute0("collapsed," + counter.name().toLowerCase());
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
            return execute0(maxTraces == 0 ? "traces" : "traces=" + maxTraces);
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
            return execute0(maxMethods == 0 ? "flat" : "flat=" + maxMethods);
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
        if (thread == null || thread == Thread.currentThread()) {
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

    private native String execute0(String command) throws IllegalArgumentException, IllegalStateException, IOException;

    private native void filterThread0(Thread thread, boolean enable);
}
