package com.taobao.arthas.core.command.monitor200;

import com.alibaba.fastjson2.JSON;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.command.view.WatchView;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.impl.http.api.ObjectVOFilter;
import com.taobao.middleware.cli.CommandLine;
import org.junit.Assert;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WatchAdviceListenerTest {

    @Test
    public void beforeWatchResultKeepsEntrySnapshotWhenArgumentsAreMutatedLater() throws Throwable {
        WatchCommand command = new WatchCommand();
        command.setBefore(true);
        command.setExpress("params");
        command.setExpand(2);

        CapturingCommandProcess process = new CapturingCommandProcess();

        WatchAdviceListener listener = new WatchAdviceListener(command, process, false);
        MutableRequest request = new MutableRequest(62, "62*4572752.0.0.7.20220506102153");

        listener.before(WatchAdviceListenerTest.class.getClassLoader(), MutableTarget.class,
                new ArthasMethod(MutableTarget.class, "observed", "()V"), new MutableTarget(),
                new Object[] { request, Boolean.TRUE });

        request.appId = 100000;
        request.appVersion = "4572752.0.0.7.20220506102153";

        WatchModel model = (WatchModel) process.result;
        ObjectVO value = model.getValue();
        Assert.assertNull(value.getObject());

        new WatchView().draw(process, model);
        String terminalResult = process.output.toString();
        Assert.assertTrue(terminalResult.contains("appId=@Integer[62]"));
        Assert.assertTrue(terminalResult.contains("appVersion=@String[62*4572752.0.0.7.20220506102153]"));
        Assert.assertFalse(terminalResult.contains("appId=@Integer[100000]"));

        String httpResult = JSON.toJSONString(model, new ObjectVOFilter());

        Assert.assertTrue(httpResult.contains("\"accessPoint\":\"AtEnter\""));
        Assert.assertTrue(httpResult.contains("appId=@Integer[62]"));
        Assert.assertTrue(httpResult.contains("appVersion=@String[62*4572752.0.0.7.20220506102153]"));
        Assert.assertFalse(httpResult.contains("appId=@Integer[100000]"));
    }

    @Test
    public void beforeWatchResultKeepsEntrySnapshotWithoutExpansion() throws Throwable {
        WatchCommand command = new WatchCommand();
        command.setBefore(true);
        command.setExpress("params[0]");
        command.setExpand(0);

        CapturingCommandProcess process = new CapturingCommandProcess();
        WatchAdviceListener listener = new WatchAdviceListener(command, process, false);
        StringBuilder value = new StringBuilder("before");

        listener.before(WatchAdviceListenerTest.class.getClassLoader(), MutableTarget.class,
                new ArthasMethod(MutableTarget.class, "observed", "()V"), new MutableTarget(),
                new Object[] { value, Boolean.TRUE });

        value.append("-after");

        ObjectVO objectVO = ((WatchModel) process.result).getValue();
        Assert.assertNull(objectVO.getObject());
        Assert.assertEquals("before", objectVO.getRenderedValue());
    }

    private static class MutableTarget {
        @SuppressWarnings("unused")
        public void observed(MutableRequest request, boolean flag) {
        }
    }

    private static class MutableRequest {
        private Integer appId;
        private String appVersion;

        private MutableRequest(Integer appId, String appVersion) {
            this.appId = appId;
            this.appVersion = appVersion;
        }
    }

    private static class CapturingCommandProcess implements CommandProcess {
        private final AtomicInteger times = new AtomicInteger();
        private final StringBuilder output = new StringBuilder();
        private ResultModel result;

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
        public CommandProcess write(String data) {
            output.append(data);
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
            this.result = result;
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
