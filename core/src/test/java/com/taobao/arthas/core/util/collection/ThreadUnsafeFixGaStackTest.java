package com.taobao.arthas.core.util.collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for ThreadUnsafeFixGaStack capacity guard.
 * The bug: checkForPush() checks (current == max) but should check (current == max - 1).
 * Since current starts at -1 and is pre-incremented before array access,
 * the guard never fires. The real AIOOBE comes from elementArray[++current].
 * After a failed push, current is corrupted (set to max), breaking subsequent operations.
 */
public class ThreadUnsafeFixGaStackTest {

    /**
     * After pushing max elements and attempting one more, peek() should still
     * return the top element (the failed push should NOT corrupt state).
     * 
     * With the bug: push() increments current past capacity before the JVM's
     * array bounds check throws, corrupting current. So peek() also fails.
     * 
     * With the fix: checkForPush() fires BEFORE ++current, so current is
     * unchanged and peek() works fine.
     */
    @Test
    public void testPeekAfterFailedPush() {
        ThreadUnsafeFixGaStack<String> stack = new ThreadUnsafeFixGaStack<>(3);
        
        // Fill the stack to capacity
        stack.push("a");
        stack.push("b");
        stack.push("c");
        
        Assert.assertEquals("c", stack.peek());
        
        // Attempt push past capacity - should throw ArrayIndexOutOfBoundsException
        try {
            stack.push("d");
            Assert.fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // Expected - the guard (or array bounds) should reject this
        }
        
        // After the failed push, peek should still work and return "c"
        // This proves the guard fired BEFORE corrupting current
        String top = stack.peek();
        Assert.assertEquals("peek() should still return top element after failed push", "c", top);
    }
    
    /**
     * After pushing max elements and attempting one more, pop() should still work.
     */
    @Test
    public void testPopAfterFailedPush() {
        ThreadUnsafeFixGaStack<String> stack = new ThreadUnsafeFixGaStack<>(3);
        
        stack.push("a");
        stack.push("b");
        stack.push("c");
        
        // Attempt push past capacity
        try {
            stack.push("d");
            Assert.fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }
        
        // After the failed push, pop should still work
        String top = stack.pop();
        Assert.assertEquals("pop() should still return top element after failed push", "c", top);
    }
    
    /**
     * Verify the stack accepts exactly max elements.
     */
    @Test
    public void testCapacityLimit() {
        ThreadUnsafeFixGaStack<Integer> stack = new ThreadUnsafeFixGaStack<>(2);
        
        stack.push(1);
        stack.push(2);
        
        Assert.assertFalse(stack.isEmpty());
        Assert.assertEquals(2, (int) stack.pop());
        Assert.assertEquals(1, (int) stack.pop());
        Assert.assertTrue(stack.isEmpty());
    }
}
