package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Regression test for ResultUtils.processClassNames() ignoring the boolean return value from PaginationHandler.handle().
 * Per the javadoc, handle() returning false should terminate processing of remaining segments.
 */
public class ResultUtilsTest {

    @Test
    public void testHandleReturnFalseStopsProcessing() {
        // Create enough classes to produce 3 segments with pageSize=2
        List<Class<?>> classes = new ArrayList<>();
        classes.add(String.class);
        classes.add(Integer.class);
        classes.add(Long.class);
        classes.add(Double.class);
        classes.add(Boolean.class);

        final AtomicInteger callCount = new AtomicInteger(0);

        ResultUtils.processClassNames(classes, 2, new ResultUtils.PaginationHandler<List<String>>() {
            @Override
            public boolean handle(List<String> classNames, int segment) {
                callCount.incrementAndGet();
                // Return false on the very first call to signal "stop processing"
                return false;
            }
        });

        // Correct behavior: when handle() returns false, no further segments should be processed.
        // The handler should have been called exactly once.
        // Buggy behavior: the return value is ignored, so all 3 segments get processed (callCount = 3).
        Assert.assertEquals("Handler should be called only once when it returns false to stop processing", 1, callCount.get());
    }
}
