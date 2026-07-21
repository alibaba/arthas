package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.command.monitor200.MonitorCommand;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.view.ResultView;
import com.taobao.arthas.core.command.view.ResultViewResolver;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.distribution.impl.TermResultDistributorImpl;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.NoOpHandler;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import io.termd.core.function.Function;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessImplConcurrencyTest {

    private static final ResultDistributor DISCARD_RESULT_DISTRIBUTOR = new ResultDistributor() {
        @Override
        public void appendResult(ResultModel result) {
        }

        @Override
        public void close() {
        }
    };

    @Test
    public void testRunShouldSerializeSharedCliParsing() throws Exception {
        final Command sharedCommand = Command.create(MonitorCommand.class);
        final int workerCount = 8;
        final int iterations = 1000;
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<?>> workers = new ArrayList<Future<?>>(workerCount);

        try {
            for (int worker = 0; worker < workerCount; worker++) {
                workers.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            start.await();
                            for (int iteration = 0; iteration < iterations; iteration++) {
                                Tty tty = new MockTty();
                                ProcessImpl process = new ProcessImpl(sharedCommand,
                                        Collections.singletonList(CliTokens.createText("-h")),
                                        new NoOpHandler<CommandProcess>(),
                                        new ProcessImpl.ProcessOutput(Collections.<Function<String, String>>emptyList(), null, tty),
                                        DISCARD_RESULT_DISTRIBUTOR);
                                process.setTty(tty);
                                process.run(false);
                                Assert.assertEquals(ExecStatus.TERMINATED, process.status());
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                    }
                }));
            }

            start.countDown();
            for (Future<?> worker : workers) {
                worker.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            Assert.assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testAppendResultShouldNotDeadlockWithProcessMonitor() throws Exception {
        ProcessImpl processImpl = new ProcessImpl(null, Collections.emptyList(), null,
                new ProcessImpl.ProcessOutput(Collections.<Function<String, String>>emptyList(), null, new MockTty()),
                null);
        setField(processImpl, "processStatus", ExecStatus.RUNNING);

        CommandProcess commandProcess = newCommandProcess(processImpl, new MockTty());
        CountDownLatch monitorHeld = new CountDownLatch(1);
        CountDownLatch drawStarted = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        ResultViewResolver resolver = new ResultViewResolver() {
            @Override
            public ResultView getResultView(ResultModel model) {
                return new ResultView<ResultModel>() {
                    @Override
                    public void draw(CommandProcess process, ResultModel result) {
                        drawStarted.countDown();
                        process.write("test");
                    }
                };
            }
        };

        TermResultDistributorImpl distributor = new TermResultDistributorImpl(commandProcess, resolver);

        Thread monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (processImpl) {
                        monitorHeld.countDown();
                        Assert.assertTrue(drawStarted.await(3, TimeUnit.SECONDS));
                        distributor.appendResult(new MessageModel("second"));
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        }, "process-monitor-thread");
        monitorThread.setDaemon(true);

        Thread outputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Assert.assertTrue(monitorHeld.await(3, TimeUnit.SECONDS));
                    distributor.appendResult(new MessageModel("first"));
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        }, "process-output-thread");
        outputThread.setDaemon(true);

        monitorThread.start();
        outputThread.start();

        monitorThread.join(TimeUnit.SECONDS.toMillis(3));
        outputThread.join(TimeUnit.SECONDS.toMillis(3));

        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }

        Assert.assertFalse("monitor thread should complete", monitorThread.isAlive());
        Assert.assertFalse("output thread should complete", outputThread.isAlive());
    }

    private static CommandProcess newCommandProcess(ProcessImpl processImpl, Tty tty) throws Exception {
        Constructor<?> constructor = Class
                .forName("com.taobao.arthas.core.shell.system.impl.ProcessImpl$CommandProcessImpl")
                .getDeclaredConstructor(ProcessImpl.class, Process.class, Tty.class);
        constructor.setAccessible(true);
        return (CommandProcess) constructor.newInstance(processImpl, processImpl, tty);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class MockTty implements Tty {
        @Override
        public String type() {
            return "test";
        }

        @Override
        public int width() {
            return 120;
        }

        @Override
        public int height() {
            return 40;
        }

        @Override
        public Tty stdinHandler(Handler<String> handler) {
            return this;
        }

        @Override
        public Tty write(String data) {
            return this;
        }

        @Override
        public Tty resizehandler(Handler<Void> handler) {
            return this;
        }
    }
}
