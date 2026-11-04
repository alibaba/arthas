package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.command.model.TimeTunnelModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeTunnelCommandTest {

    public static class TestTarget {
        public String echo(String value) {
            return value;
        }
    }

    @After
    public void clearTimeTunnelState() throws Exception {
        Field timeFragmentMapField = TimeTunnelCommand.class.getDeclaredField("timeFragmentMap");
        timeFragmentMapField.setAccessible(true);
        ((Map<?, ?>) timeFragmentMapField.get(null)).clear();

        Field sequenceField = TimeTunnelCommand.class.getDeclaredField("sequence");
        sequenceField.setAccessible(true);
        ((AtomicInteger) sequenceField.get(null)).set(1000);
    }

    @Test
    public void shouldCreateTimeFragmentVOWhenAdviceParamsIsNull() {
        ArthasMethod method = new ArthasMethod(TestTarget.class, "echo", "(Ljava/lang/String;)Ljava/lang/String;");
        Advice advice = Advice.newForAfterReturning(TestTarget.class.getClassLoader(), TestTarget.class, method,
                new TestTarget(), null, "ok");
        TimeFragment timeFragment = new TimeFragment(advice, LocalDateTime.now(), 1.0d);

        TimeFragmentVO timeFragmentVO = TimeTunnelCommand.createTimeFragmentVO(1000, timeFragment, 1);

        Assert.assertNotNull(timeFragmentVO);
        Assert.assertNotNull(timeFragmentVO.getParams());
        Assert.assertEquals(0, timeFragmentVO.getParams().length);
    }

    @Test
    public void shouldFallbackToExitArgsWhenSnapshotIsMissing() throws Throwable {
        TimeTunnelCommand command = new TimeTunnelCommand();
        CommandProcess process = Mockito.mock(CommandProcess.class);
        AtomicInteger times = new AtomicInteger();
        Mockito.when(process.times()).thenReturn(times);

        TimeTunnelAdviceListener listener = new TimeTunnelAdviceListener(command, process, false);
        Object[] args = new Object[] { "input" };
        listener.afterReturning(TestTarget.class.getClassLoader(), TestTarget.class,
                new ArthasMethod(TestTarget.class, "echo", "(Ljava/lang/String;)Ljava/lang/String;"),
                new TestTarget(), args, "ok");

        ArgumentCaptor<ResultModel> resultCaptor = ArgumentCaptor.forClass(ResultModel.class);
        Mockito.verify(process).appendResult(resultCaptor.capture());

        TimeTunnelModel timeTunnelModel = (TimeTunnelModel) resultCaptor.getValue();
        Assert.assertNotNull(timeTunnelModel.getTimeFragmentList());
        Assert.assertEquals(1, timeTunnelModel.getTimeFragmentList().size());

        TimeFragmentVO timeFragmentVO = timeTunnelModel.getTimeFragmentList().get(0);
        Assert.assertNotNull(timeFragmentVO.getParams());
        Assert.assertEquals(1, timeFragmentVO.getParams().length);
        Assert.assertEquals("input", timeFragmentVO.getParams()[0].getObject());
    }
}
