package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.arthas.core.util.ThreadLocalWatch.LongStack;

/**
 * 
 * @author hengyunabc 2019-11-20
 *
 */
public class LongStackTest {

    @Test
    public void test() {
        LongStack stack = new LongStack(100);

        stack.push(1);
        stack.push(2);
        stack.push(3);

        Assert.assertEquals(3, stack.pop());
        Assert.assertEquals(2, stack.pop());
        Assert.assertEquals(1, stack.pop());
    }

    @Test
    public void test2() {
        LongStack stack = new LongStack(100);

        stack.push(1);
        stack.push(2);
        stack.push(3);

        Assert.assertEquals(3, stack.pop());
        Assert.assertEquals(2, stack.pop());
        Assert.assertEquals(1, stack.pop());
        Assert.assertEquals(0, stack.pop());
    }

    @Test
    public void test3() {
        LongStack stack = new LongStack(2);

        stack.push(1);
        stack.push(2);
        stack.push(3);

        Assert.assertEquals(3, stack.pop());
        Assert.assertEquals(2, stack.pop());
        Assert.assertEquals(3, stack.pop());
        Assert.assertEquals(2, stack.pop());
    }

    @Test
    public void test4() {
        LongStack stack = new LongStack(2);

        stack.push(1);
        stack.push(2);

        Assert.assertEquals(2, stack.pop());
        Assert.assertEquals(1, stack.pop());
        Assert.assertEquals(2, stack.pop());
        Assert.assertEquals(1, stack.pop());
    }
    
    @Test
    public void test5() {
        LongStack stack = new LongStack(10);

        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.pop();
        stack.pop();
        stack.push(4);
        stack.push(5);
        
        stack.push(6);
        stack.pop();

        Assert.assertEquals(5, stack.pop());
        Assert.assertEquals(4, stack.pop());
        Assert.assertEquals(1, stack.pop());
        Assert.assertEquals(0, stack.pop());
    }
}
