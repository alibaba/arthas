package com.taobao.arthas.core.command.startup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.util.DateUtils;

/**
 * Writes one startup command's output as JSON Lines without blocking application threads on disk IO.
 */
class StartupCommandResultWriter implements ResultDistributor {
    static final int DEFAULT_QUEUE_CAPACITY = 1024;

    private static final Logger logger = LoggerFactory.getLogger(StartupCommandResultWriter.class);

    private final int commandIndex;
    private final String command;
    private final File outputFile;
    private final BlockingQueue<Event> queue;
    private final Object queueLock = new Object();
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicLong droppedEvents = new AtomicLong(0);
    private final AtomicLong sequence = new AtomicLong(0);
    private final CountDownLatch closed = new CountDownLatch(1);
    private final BufferedWriter writer;
    private final Thread writerThread;

    StartupCommandResultWriter(int commandIndex, String command, File outputFile) throws IOException {
        this(commandIndex, command, outputFile, DEFAULT_QUEUE_CAPACITY);
    }

    StartupCommandResultWriter(int commandIndex, String command, File outputFile, int queueCapacity)
                    throws IOException {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than 0");
        }
        this.commandIndex = commandIndex;
        this.command = command;
        this.outputFile = outputFile;
        this.queue = new ArrayBlockingQueue<Event>(queueCapacity);
        this.writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        this.writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeEvents();
            }
        }, String.format("arthas-startup-result-%03d", commandIndex));
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    @Override
    public void appendResult(ResultModel result) {
        if (result != null) {
            enqueue(Event.result(result));
        }
    }

    void appendStdout(String text) {
        if (text != null && text.length() > 0) {
            enqueue(Event.stdout(text));
        }
    }

    void appendLifecycle(String event) {
        appendLifecycle(event, null);
    }

    void appendLifecycle(String event, Map<String, Object> data) {
        enqueue(Event.lifecycle(event, data));
    }

    private void enqueue(Event event) {
        synchronized (queueLock) {
            if (closing.get()) {
                return;
            }
            if (!queue.offer(event)) {
                queue.poll();
                droppedEvents.incrementAndGet();
                if (!queue.offer(event)) {
                    droppedEvents.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (queueLock) {
            closing.set(true);
        }
    }

    boolean awaitClosed(long timeout, TimeUnit unit) throws InterruptedException {
        return closed.await(timeout, unit);
    }

    private void writeEvents() {
        try {
            while (true) {
                Event event = queue.poll(100, TimeUnit.MILLISECONDS);
                writeDroppedEventsIfNecessary();
                if (event == null) {
                    synchronized (queueLock) {
                        if (closing.get() && queue.isEmpty()) {
                            break;
                        }
                    }
                    continue;
                }
                writeEvent(event);
            }
            writer.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            logger.error("Write startup command result failed: {}", outputFile, e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                logger.warn("Close startup command result file failed: {}", outputFile, e);
            }
            closed.countDown();
        }
    }

    private void writeDroppedEventsIfNecessary() throws IOException {
        long count = droppedEvents.getAndSet(0);
        if (count <= 0) {
            return;
        }
        Map<String, Object> envelope = baseEnvelope(DateUtils.getCurrentDateTime(), "resultsDropped");
        envelope.put("count", count);
        writeJsonLine(envelope);
    }

    private void writeEvent(Event event) throws IOException {
        Map<String, Object> envelope = baseEnvelope(event.timestamp, event.type);
        if (event.result != null) {
            envelope.put("resultType", event.result.getType());
            envelope.put("result", event.result);
        }
        if (event.stdout != null) {
            envelope.put("stdout", event.stdout);
        }
        if (event.data != null && !event.data.isEmpty()) {
            envelope.put("data", event.data);
        }
        try {
            writeJsonLine(envelope);
        } catch (Throwable e) {
            Map<String, Object> fallback = baseEnvelope(event.timestamp, "serializationError");
            fallback.put("sourceEvent", event.type);
            fallback.put("error", e.toString());
            writeJsonLine(fallback);
        }
    }

    private Map<String, Object> baseEnvelope(String timestamp, String event) {
        Map<String, Object> envelope = new LinkedHashMap<String, Object>();
        envelope.put("sequence", sequence.incrementAndGet());
        envelope.put("timestamp", timestamp);
        envelope.put("commandIndex", commandIndex);
        envelope.put("command", command);
        envelope.put("event", event);
        return envelope;
    }

    private void writeJsonLine(Map<String, Object> value) throws IOException {
        writer.write(JSON.toJSONString(value));
        writer.newLine();
        writer.flush();
    }

    private static class Event {
        private final String timestamp;
        private final String type;
        private final ResultModel result;
        private final String stdout;
        private final Map<String, Object> data;

        private Event(String timestamp, String type, ResultModel result, String stdout, Map<String, Object> data) {
            this.timestamp = timestamp;
            this.type = type;
            this.result = result;
            this.stdout = stdout;
            this.data = data;
        }

        private static Event result(ResultModel result) {
            return new Event(DateUtils.getCurrentDateTime(), "result", result, null, null);
        }

        private static Event stdout(String text) {
            return new Event(DateUtils.getCurrentDateTime(), "stdout", null, text, null);
        }

        private static Event lifecycle(String type, Map<String, Object> data) {
            return new Event(DateUtils.getCurrentDateTime(), type, null, null, data);
        }
    }
}
