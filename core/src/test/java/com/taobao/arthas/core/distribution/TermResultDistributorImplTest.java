package com.taobao.arthas.core.distribution;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.command.view.ResultView;
import com.taobao.arthas.core.command.view.ResultViewResolver;
import com.taobao.arthas.core.command.view.WatchView;
import com.taobao.arthas.core.distribution.impl.TermResultDistributorImpl;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.middleware.cli.CommandLine;
import org.junit.Assert;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TermResultDistributorImplTest {

    @Test
    public void testConcurrentOutputNotInterleaved() throws InterruptedException {
        final StringBuilder outputCollector = new StringBuilder();

        CommandProcess mockProcess = new MockCommandProcess(outputCollector);

        ResultViewResolver resolver = new ResultViewResolver() {
            @Override
            public ResultView getResultView(ResultModel model) {
                if (model instanceof WatchModel) {
                    return new WatchView();
                }
                return null;
            }
        };

        TermResultDistributorImpl distributor = new TermResultDistributorImpl(mockProcess, resolver);

        int threadCount = 50;
        int outputPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int i = 0; i < outputPerThread; i++) {
                        WatchModel model = new WatchModel();
                        model.setTs(LocalDateTime.now());
                        model.setCost(1.5);
                        model.setClassName("TestClass");
                        model.setMethodName("testMethod");
                        model.setAccessPoint("AtExit");

                        List<String> params = new ArrayList<>();
                        params.add("Thread-" + threadId + "-Item-" + i + "-A");
                        params.add("Thread-" + threadId + "-Item-" + i + "-B");
                        params.add("Thread-" + threadId + "-Item-" + i + "-C");
                        
                        model.setValue(new ObjectVO(params, 2));
                        model.setSizeLimit(10 * 1024 * 1024);
                        
                        distributor.appendResult(model);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        endLatch.await();
        executor.shutdown();

        String output = outputCollector.toString();

        Pattern blockPattern = Pattern.compile(
            "method=TestClass\\.testMethod location=AtExit\\n" +
            "ts=[^;]+; \\[cost=[\\d.]+ms\\] result=@ArrayList\\[\\n" +
            "    @String\\[Thread-\\d+-Item-\\d+-A\\],\\n" +
            "    @String\\[Thread-\\d+-Item-\\d+-B\\],\\n" +
            "    @String\\[Thread-\\d+-Item-\\d+-C\\],\\n" +
            "\\]\\n"
        );
        
        Matcher matcher = blockPattern.matcher(output);
        int completeBlockCount = 0;
        while (matcher.find()) {
            completeBlockCount++;
        }
        
        int expectedBlockCount = threadCount * outputPerThread;

        Assert.assertEquals("All output blocks should be complete and not interleaved", 
                expectedBlockCount, completeBlockCount);

        Pattern interleavedPattern = Pattern.compile("Thread-\\d+-Item-\\d+-[ABC]\\],\\nmethod=");
        Matcher interleavedMatcher = interleavedPattern.matcher(output);
        Assert.assertFalse("Output should not be interleaved between different results", 
                interleavedMatcher.find());
    }

    private static class MockCommandProcess implements CommandProcess {
        private final StringBuilder outputCollector;
        private final AtomicInteger times = new AtomicInteger();
        
        public MockCommandProcess(StringBuilder outputCollector) {
            this.outputCollector = outputCollector;
        }
        
        @Override
        public CommandProcess write(String data) {
            synchronized (outputCollector) {
                outputCollector.append(data);
            }
            return this;
        }
        
        @Override
        public List<CliToken> argsTokens() {
            return null;
        }

        @Override
        public List<String> args() {
            return null;
        }

        @Override
        public CommandLine commandLine() {
            return null;
        }

        @Override
        public Session session() {
            return null;
        }

        @Override
        public boolean isForeground() {
            return true;
        }

        @Override
        public CommandProcess stdinHandler(Handler<String> handler) {
            return this;
        }

        @Override
        public CommandProcess interruptHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public CommandProcess suspendHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public CommandProcess resumeHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public CommandProcess endHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public CommandProcess backgroundHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public CommandProcess foregroundHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public CommandProcess resizehandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public void end() {
        }

        @Override
        public void end(int status) {
        }

        @Override
        public void end(int status, String message) {
        }

        @Override
        public void register(AdviceListener listener, ClassFileTransformer transformer) {
        }

        @Override
        public void unregister() {
        }

        @Override
        public AtomicInteger times() {
            return times;
        }

        @Override
        public void resume() {
        }

        @Override
        public void suspend() {
        }

        @Override
        public void echoTips(String tips) {
        }

        @Override
        public String cacheLocation() {
            return null;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public void appendResult(ResultModel result) {
        }

        @Override
        public String type() {
            return "test";
        }

        @Override
        public int width() {
            return 80;
        }

        @Override
        public int height() {
            return 24;
        }
    }
}
