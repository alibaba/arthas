package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ThreadVO;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ViewRenderUtilTest {

    @Test
    public void testLongThreadIdIsFullyVisibleAtDefaultWidth() {
        String output = render(80, thread(100123L, "vert.x-eventloop-thread-1"));

        Assert.assertTrue("thread id truncated:\n" + output, containsId(output, "100123"));
        Assert.assertTrue(output.contains("DAEMON"));
    }

    @Test
    public void testIdsThatShareAPrefixStayDistinct() {
        String output = render(80, thread(100L, "pool-1"), thread(101L, "pool-2"));

        Assert.assertTrue("id 100 truncated:\n" + output, containsId(output, "100"));
        Assert.assertTrue("id 101 truncated:\n" + output, containsId(output, "101"));
    }

    @Test
    public void testShortThreadIdStillRenders() {
        String output = render(80, thread(-1L, "Sweeper thread"));

        Assert.assertTrue(containsId(output, "-1"));
        Assert.assertTrue(output.contains("Sweeper"));
        Assert.assertTrue(output.contains("DAEMON"));
    }

    private static String render(int width, ThreadVO... threads) {
        return ViewRenderUtil.drawThreadInfo(Arrays.asList(threads), width, 10);
    }

    private static ThreadVO thread(long id, String name) {
        ThreadVO thread = new ThreadVO();
        thread.setId(id);
        thread.setName(name);
        thread.setGroup("main");
        thread.setPriority(5);
        thread.setState(Thread.State.RUNNABLE);
        thread.setCpu(0.0);
        thread.setDeltaTime(0L);
        thread.setTime(0L);
        thread.setInterrupted(false);
        thread.setDaemon(false);
        return thread;
    }

    private static boolean containsId(String output, String id) {
        String plain = output.replaceAll("\u001B\\[[0-9;]*m", "");
        return plain.contains(id);
    }
}
