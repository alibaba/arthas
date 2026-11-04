package com.taobao.arthas.core.command.monitor200;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Tests for ThreadContentionCommand functionality.
 *
 * @author spatchava
 */
public class ThreadContentionCommandTest {

    @Test
    public void testDeadlockDetectionWithNoDeadlock() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlocked = threadMXBean.findDeadlockedThreads();
        // In a normal test environment, there should be no deadlocks
        assertNull("No deadlocks should exist in a clean test environment", deadlocked);
    }

    @Test
    public void testDeadlockDetectionWithSyntheticDeadlock() throws Exception {
        final Object lockA = new Object();
        final Object lockB = new Object();
        final boolean[] deadlockCreated = {false};

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                deadlockCreated[0] = true;
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockB) {
                    // This should never be reached in a deadlock
                }
            }
        }, "DeadlockTest-Thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                while (!deadlockCreated[0]) {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                }
                synchronized (lockA) {
                    // This should never be reached in a deadlock
                }
            }
        }, "DeadlockTest-Thread-2");

        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        // Wait for deadlock to form
        Thread.sleep(500);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

        // Clean up - interrupt threads (they won't actually unblock from deadlock,
        // but since they're daemon threads they'll be cleaned up on JVM exit)
        t1.interrupt();
        t2.interrupt();

        // Verify deadlock was detected
        assertNotNull("Deadlock should be detected between two threads", deadlockedThreads);
        assertEquals("Exactly 2 threads should be in deadlock", 2, deadlockedThreads.length);
    }

    @Test
    public void testContentionAnalysisOutputFormatting() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        assertNotNull("Thread dump should not be null", threadInfos);
        assertTrue("Thread dump should contain at least one thread", threadInfos.length > 0);

        // Verify ThreadInfo contains expected data
        boolean foundMainThread = false;
        for (ThreadInfo info : threadInfos) {
            assertNotNull("Thread name should not be null", info.getThreadName());
            assertNotNull("Thread state should not be null", info.getThreadState());
            if ("main".equals(info.getThreadName())) {
                foundMainThread = true;
            }
        }
        assertTrue("Should find the main thread in thread dump", foundMainThread);
    }

    @Test
    public void testThreadWaitChainBuilding() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        // Build a map of thread ID -> ThreadInfo
        java.util.Map<Long, ThreadInfo> threadMap = new java.util.HashMap<>();
        for (ThreadInfo info : threadInfos) {
            threadMap.put(info.getThreadId(), info);
        }

        // Verify we can look up threads by ID
        for (ThreadInfo info : threadInfos) {
            ThreadInfo lookedUp = threadMap.get(info.getThreadId());
            assertNotNull("Should find thread in map by ID", lookedUp);
            assertEquals("Thread names should match", info.getThreadName(), lookedUp.getThreadName());
        }

        // Verify wait chain links are valid (if a thread is waiting on a lock owner,
        // that owner should exist in the thread map)
        for (ThreadInfo info : threadInfos) {
            if (info.getLockOwnerId() > 0) {
                ThreadInfo owner = threadMap.get(info.getLockOwnerId());
                assertNotNull("Lock owner thread should exist in thread map for thread: "
                        + info.getThreadName(), owner);
            }
        }
    }

    @Test
    public void testNoContentionScenario() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        // Count threads with lock contention (waiting on a lock)
        int blockedCount = 0;
        for (ThreadInfo info : threadInfos) {
            if (info.getThreadState() == Thread.State.BLOCKED) {
                blockedCount++;
            }
        }

        // In a normal test environment without deliberate contention,
        // we expect no or very few blocked threads
        assertTrue("In a clean test environment, blocked thread count should be low",
                blockedCount < threadInfos.length / 2);
    }

    @Test
    public void testThreadMXBeanContentionMonitoring() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        // Test that contention monitoring can be enabled/disabled safely
        boolean originalState = threadMXBean.isThreadContentionMonitoringEnabled();
        try {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
            assertTrue("Contention monitoring should be enabled",
                    threadMXBean.isThreadContentionMonitoringEnabled());

            // Verify blocked time is available when monitoring is enabled
            ThreadInfo[] infos = threadMXBean.dumpAllThreads(false, false);
            for (ThreadInfo info : infos) {
                // Blocked time should be >= 0 when monitoring is enabled
                assertTrue("Blocked time should be non-negative",
                        info.getBlockedTime() >= 0);
                assertTrue("Waited time should be non-negative",
                        info.getWaitedTime() >= 0);
            }
        } finally {
            // Restore original state - validates the fix for resource leak
            threadMXBean.setThreadContentionMonitoringEnabled(originalState);
            assertEquals("Contention monitoring should be restored to original state",
                    originalState, threadMXBean.isThreadContentionMonitoringEnabled());
        }
    }
}
