/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Records latency spans into the async-profiler JFR recording.
 * <p>
 * A span is a {@code [start, end]} time interval on the current thread, labeled with a tag
 * (e.g. an operation or endpoint name). Spans are written into the same JFR file as the profiling
 * samples, so the profile can later be filtered by span duration or tag. When async-profiler is
 * not loaded or no JFR session is running, all methods are cheap no-ops.
 */
public class Span {
    // Long.MAX_VALUE as the last profiler timestamp forces recording of every span
    private static final ByteBuffer FALLBACK_BUF =
            ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).putLong(0, Long.MAX_VALUE);

    private static final Class<?> VIRTUAL_THREAD_CLASS = getVirtualThreadClass();

    private static Class<?> getVirtualThreadClass() {
        try {
            return Class.forName("java.lang.BaseVirtualThread");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean isVirtualThread() {
        return VIRTUAL_THREAD_CLASS != null && VIRTUAL_THREAD_CLASS.isInstance(Thread.currentThread());
    }

    // Carries timestamp of the last profiling event in the current thread
    private static final ThreadLocal<ByteBuffer> LOCAL_BUF = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            ByteBuffer buf = Recording.getThreadLocalBuffer();
            return buf != null ? buf.order(ByteOrder.nativeOrder()) : FALLBACK_BUF;
        }
    };

    private static boolean hasProfileSamples(long startTime) {
        return LOCAL_BUF.get().getLong(0) >= startTime;
    }

    private Span() {
    }

    /**
     * Marks the start of a span on the current thread.
     *
     * @return a start timestamp to pass to {@link #end} / {@link #endIfProfiled},
     *         or {@code 0} if async-profiler is not loaded
     */
    public static long start() {
        if (Recording.state != Recording.RUNNING) {
            return 0;
        }
        if (!isVirtualThread()) {
            LOCAL_BUF.get();  // force creation of a thread-local buffer
        }
        return Recording.timestamp();
    }

    /**
     * Ends a span opened with {@link #start} and records it with the given tag.
     *
     * @param startTime the value returned by {@link #start}
     * @param tag       an arbitrary label, or {@code null}
     */
    public static void end(long startTime, String tag) {
        if (startTime != 0 && Recording.state == Recording.RUNNING) {
            Recording.emitSpan(startTime, Recording.timestamp(), tag);
        }
    }

    /**
     * Like {@link #end}, but records the span only if at least one profiling sample was taken on
     * this thread while it was open. Useful for very frequent spans: those enclosing no sample add
     * nothing to the profile and are skipped without entering native code.
     *
     * @param startTime the value returned by {@link #start}
     * @param tag       an arbitrary label, or {@code null}
     */
    public static void endIfProfiled(long startTime, String tag) {
        if (startTime != 0 && Recording.state == Recording.RUNNING) {
            if (isVirtualThread() || hasProfileSamples(startTime)) {
                Recording.emitSpan(startTime, Recording.timestamp(), tag);
            }
        }
    }

    /**
     * Records a span with explicit start and end timestamps obtained from
     * {@link Recording#timestamp()} or converted from another clock
     * using {@link Recording#clockFrequency()}.
     *
     * @param tag an arbitrary label, or {@code null}
     */
    public static void emit(long startTime, long endTime, String tag) {
        if (Recording.state == Recording.RUNNING) {
            Recording.emitSpan(startTime, endTime, tag);
        }
    }

    /**
     * Like {@link #emit}, but records the span only if a profiling sample was taken on this thread
     * within {@code [startTime, endTime]}.
     *
     * @param tag an arbitrary label, or {@code null}
     */
    public static void emitIfProfiled(long startTime, long endTime, String tag) {
        if (Recording.state == Recording.RUNNING) {
            if (isVirtualThread() || hasProfileSamples(startTime)) {
                Recording.emitSpan(startTime, endTime, tag);
            }
        }
    }
}
