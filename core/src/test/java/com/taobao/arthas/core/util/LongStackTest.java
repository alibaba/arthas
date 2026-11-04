package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author hengyunabc 2019-11-20
 *
 */
public class LongStackTest {

    @Test
    public void test() {
        long[] stack = new long[101];

        ThreadLocalWatch.push(stack, 1);
        ThreadLocalWatch.push(stack, 2);
        ThreadLocalWatch.push(stack, 3);

        Assert.assertEquals(3, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(2, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(1, ThreadLocalWatch.pop(stack));
    }

    @Test
    public void test2() {
        long[] stack = new long[101];

        ThreadLocalWatch.push(stack, 1);
        ThreadLocalWatch.push(stack, 2);
        ThreadLocalWatch.push(stack, 3);

        Assert.assertEquals(3, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(2, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(1, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(0, ThreadLocalWatch.pop(stack));
    }

    @Test
    public void test3() {
        long[] stack = new long[3];

        ThreadLocalWatch.push(stack, 1);
        ThreadLocalWatch.push(stack, 2);
        ThreadLocalWatch.push(stack, 3);

        Assert.assertEquals(3, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(2, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(3, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(2, ThreadLocalWatch.pop(stack));
    }

    @Test
    public void test4() {
        long[] stack = new long[3];

        ThreadLocalWatch.push(stack, 1);
        ThreadLocalWatch.push(stack, 2);

        Assert.assertEquals(2, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(1, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(2, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(1, ThreadLocalWatch.pop(stack));
    }
    
    @Test
    public void test5() {
        long[] stack = new long[11];

        ThreadLocalWatch.push(stack, 1);
        ThreadLocalWatch.push(stack, 2);
        ThreadLocalWatch.push(stack, 3);
        ThreadLocalWatch.pop(stack);
        ThreadLocalWatch.pop(stack);
        ThreadLocalWatch.push(stack, 4);
        ThreadLocalWatch.push(stack, 5);
        
        ThreadLocalWatch.push(stack, 6);
        ThreadLocalWatch.pop(stack);

        Assert.assertEquals(5, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(4, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(1, ThreadLocalWatch.pop(stack));
        Assert.assertEquals(0, ThreadLocalWatch.pop(stack));
    }
}
