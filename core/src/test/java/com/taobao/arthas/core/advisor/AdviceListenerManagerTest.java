package com.taobao.arthas.core.advisor;

import java.util.Iterator;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class AdviceListenerManagerTest {

    @Test
    public void queryAdviceListenersShouldReturnStableSnapshotWhenRegisteringAnotherListener() {
        AdviceListenerManager.ClassLoaderAdviceListenerManager manager =
                new AdviceListenerManager.ClassLoaderAdviceListenerManager();
        TestAdviceListener first = new TestAdviceListener(1);
        TestAdviceListener second = new TestAdviceListener(2);

        manager.registerAdviceListener("demo.MathGame", "primeFactors", "(I)Ljava/util/List;", first);

        List<AdviceListener> listeners = manager.queryAdviceListeners("demo.MathGame", "primeFactors",
                "(I)Ljava/util/List;");
        Iterator<AdviceListener> iterator = listeners.iterator();
        Assertions.assertThat(iterator.next()).isSameAs(first);

        manager.registerAdviceListener("demo.MathGame", "primeFactors", "(I)Ljava/util/List;", second);

        Assertions.assertThatCode(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                while (iterator.hasNext()) {
                    iterator.next();
                }
            }
        }).doesNotThrowAnyException();
        Assertions.assertThat(listeners).containsExactly(first);
        Assertions.assertThat(manager.queryAdviceListeners("demo.MathGame", "primeFactors", "(I)Ljava/util/List;"))
                .containsExactly(first, second);
    }

    @Test
    public void queryTraceAdviceListenersShouldReturnStableSnapshotWhenRegisteringAnotherListener() {
        AdviceListenerManager.ClassLoaderAdviceListenerManager manager =
                new AdviceListenerManager.ClassLoaderAdviceListenerManager();
        TestAdviceListener first = new TestAdviceListener(1);
        TestAdviceListener second = new TestAdviceListener(2);

        manager.registerTraceAdviceListener("demo.MathGame", "java/lang/StringBuilder", "append",
                "(I)Ljava/lang/StringBuilder;", first);

        List<AdviceListener> listeners = manager.queryTraceAdviceListeners("demo.MathGame", "java/lang/StringBuilder",
                "append", "(I)Ljava/lang/StringBuilder;");
        Iterator<AdviceListener> iterator = listeners.iterator();
        Assertions.assertThat(iterator.next()).isSameAs(first);

        manager.registerTraceAdviceListener("demo.MathGame", "java/lang/StringBuilder", "append",
                "(I)Ljava/lang/StringBuilder;", second);

        Assertions.assertThatCode(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                while (iterator.hasNext()) {
                    iterator.next();
                }
            }
        }).doesNotThrowAnyException();
        Assertions.assertThat(listeners).containsExactly(first);
        Assertions.assertThat(manager.queryTraceAdviceListeners("demo.MathGame", "java/lang/StringBuilder", "append",
                "(I)Ljava/lang/StringBuilder;")).containsExactly(first, second);
    }

    private static class TestAdviceListener implements AdviceListener {
        private final long id;

        private TestAdviceListener(long id) {
            this.id = id;
        }

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
        public void before(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args) {
        }

        @Override
        public void afterReturning(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
                Object returnObject) {
        }

        @Override
        public void afterThrowing(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
                Throwable throwable) {
        }
    }
}
