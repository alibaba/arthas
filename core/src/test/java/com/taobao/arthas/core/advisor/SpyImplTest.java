package com.taobao.arthas.core.advisor;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import com.taobao.arthas.core.bytecode.TestHelper;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.StringUtils;

import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * 
 * @author hengyunabc 2021-07-14
 *
 */
public class SpyImplTest {
    private static final AtomicLong LISTENER_ID = new AtomicLong(1000L);
    private static final AtomicLong METHOD_SEQUENCE = new AtomicLong(1L);

    @BeforeClass
    public static void beforeClass() throws Throwable {
        try {
            ArthasBootstrap.getInstance();
            return;
        } catch (IllegalStateException ignored) {
            // 测试首次运行时初始化 Arthas 环境，供 AdviceListenerManager 使用。
        }

        Instrumentation instrumentation = ByteBuddyAgent.install();
        TestHelper.appendSpyJar(instrumentation);
        ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");
    }

    @Test
    public void testSplitMethodInfo() throws Throwable {
        Assertions.assertThat(StringUtils.splitMethodInfo("a|b")).containsExactly("a", "b");
        Assertions.assertThat(StringUtils.splitMethodInfo("xxxxxxxxxx|fffffffffff")).containsExactly("xxxxxxxxxx",
                "fffffffffff");
        Assertions.assertThat(StringUtils.splitMethodInfo("print|(ILjava/util/List;)V")).containsExactly("print",
                "(ILjava/util/List;)V");
    }

    @Test
    public void testSplitInvokeInfo() throws Throwable {
        Assertions.assertThat(StringUtils.splitInvokeInfo("demo/MathGame|primeFactors|(I)Ljava/util/List;|24"))
                .containsExactly("demo/MathGame", "primeFactors", "(I)Ljava/util/List;", "24");

    }

    @Test
    public void shouldSkipReentrantAfterReturningDispatch() throws Throwable {
        final SpyImpl spy = new SpyImpl();
        final String methodName = nextMethodName("reentrantAfterReturning");
        final String methodDesc = "()Ljava/lang/String;";
        final String methodInfo = buildMethodInfo(methodName, methodDesc);
        final AtomicBoolean reentered = new AtomicBoolean(false);
        final AtomicInteger callbackCount = new AtomicInteger(0);
        registerMethodListener(methodName, methodDesc, new TestAdviceListener() {

            @Override
            public void afterReturning(Class<?> clazz, String adviceMethodName, String adviceMethodDesc, Object target,
                    Object[] args, Object returnObject) throws Throwable {
                callbackCount.incrementAndGet();
                if (reentered.compareAndSet(false, true)) {
                    spy.atExit(SpyImplTest.class, methodInfo, target, args, returnObject);
                }
            }
        });

        spy.atExit(SpyImplTest.class, methodInfo, null, new Object[0], "done");

        Assertions.assertThat(reentered.get()).isTrue();
        Assertions.assertThat(callbackCount).hasValue(1);
    }

    @Test
    public void shouldSkipReentrantAfterThrowingDispatch() throws Throwable {
        final SpyImpl spy = new SpyImpl();
        final String methodName = nextMethodName("reentrantAfterThrowing");
        final String methodDesc = "()V";
        final Throwable expected = new IllegalStateException("boom");
        final String methodInfo = buildMethodInfo(methodName, methodDesc);
        final AtomicBoolean reentered = new AtomicBoolean(false);
        final AtomicInteger callbackCount = new AtomicInteger(0);
        registerMethodListener(methodName, methodDesc, new TestAdviceListener() {

            @Override
            public void afterThrowing(Class<?> clazz, String adviceMethodName, String adviceMethodDesc, Object target,
                    Object[] args, Throwable throwable) throws Throwable {
                callbackCount.incrementAndGet();
                if (reentered.compareAndSet(false, true)) {
                    spy.atExceptionExit(SpyImplTest.class, methodInfo, target, args, throwable);
                }

                Assertions.assertThat(throwable).isSameAs(expected);
            }
        });

        spy.atExceptionExit(SpyImplTest.class, methodInfo, null, new Object[0], expected);

        Assertions.assertThat(reentered.get()).isTrue();
        Assertions.assertThat(callbackCount).hasValue(1);
    }

    @Test
    public void shouldSkipNestedDispatchForOtherListenersToo() throws Throwable {
        final SpyImpl spy = new SpyImpl();
        final String methodName = nextMethodName("nestedDispatchAllListeners");
        final String methodDesc = "()I";
        final AtomicInteger firstListenerCount = new AtomicInteger(0);
        final AtomicInteger secondListenerCount = new AtomicInteger(0);
        final AtomicBoolean reentered = new AtomicBoolean(false);
        final String methodInfo = buildMethodInfo(methodName, methodDesc);

        AdviceListenerManager.registerAdviceListener(SpyImplTest.class.getClassLoader(), SpyImplTest.class.getName(),
                methodName, methodDesc, new TestAdviceListener() {
                    @Override
                    public void afterReturning(Class<?> clazz, String adviceMethodName, String adviceMethodDesc,
                            Object target, Object[] args, Object returnObject) throws Throwable {
                        firstListenerCount.incrementAndGet();
                        if (reentered.compareAndSet(false, true)) {
                            spy.atExit(SpyImplTest.class, methodInfo, target, args, returnObject);
                        }
                    }
                });

        AdviceListenerManager.registerAdviceListener(SpyImplTest.class.getClassLoader(), SpyImplTest.class.getName(),
                methodName, methodDesc, new TestAdviceListener() {
                    @Override
                    public void afterReturning(Class<?> clazz, String adviceMethodName, String adviceMethodDesc,
                            Object target, Object[] args, Object returnObject) throws Throwable {
                        secondListenerCount.incrementAndGet();
                    }
                });

        spy.atExit(SpyImplTest.class, methodInfo, null, new Object[0], Integer.valueOf(1));

        Assertions.assertThat(reentered.get()).isTrue();
        Assertions.assertThat(firstListenerCount).hasValue(1);
        Assertions.assertThat(secondListenerCount).hasValue(1);
    }

    private static String registerMethodListener(String methodName, String methodDesc, AdviceListener adviceListener) {
        AdviceListenerManager.registerAdviceListener(SpyImplTest.class.getClassLoader(), SpyImplTest.class.getName(),
                methodName, methodDesc, adviceListener);
        return buildMethodInfo(methodName, methodDesc);
    }

    private static String buildMethodInfo(String methodName, String methodDesc) {
        return methodName + "|" + methodDesc;
    }

    private static String nextMethodName(String prefix) {
        return prefix + METHOD_SEQUENCE.getAndIncrement();
    }

    private abstract static class TestAdviceListener implements AdviceListener {
        private final long id = LISTENER_ID.getAndIncrement();

        @Override
        public long id() {
            return id;
        }

        @Override
        public void create() {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void before(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args)
                throws Throwable {
        }

        @Override
        public void afterReturning(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
                Object returnObject) throws Throwable {
        }

        @Override
        public void afterThrowing(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
                Throwable throwable) throws Throwable {
        }
    }
}
