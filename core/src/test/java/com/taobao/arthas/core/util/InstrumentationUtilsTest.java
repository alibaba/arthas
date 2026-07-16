package com.taobao.arthas.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assume;
import org.junit.Test;

import net.bytebuddy.agent.ByteBuddyAgent;

public class InstrumentationUtilsTest {

    @Test
    public void shouldRetransformOrdinaryClassWithLambdaInName() {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        Assume.assumeTrue(instrumentation.isRetransformClassesSupported());
        Assume.assumeTrue(instrumentation.isModifiableClass(Ordinary$$Lambda$1.class));
        final AtomicReference<byte[]> captured = new AtomicReference<byte[]>();
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classBeingRedefined == Ordinary$$Lambda$1.class) {
                    captured.set(classfileBuffer);
                }
                return null;
            }
        };

        InstrumentationUtils.retransformClasses(instrumentation, transformer,
                Collections.<Class<?>>singleton(Ordinary$$Lambda$1.class));

        assertThat(Ordinary$$Lambda$1.class.isSynthetic()).isFalse();
        assertThat(captured.get()).isNotNull().isNotEmpty();
    }

    @Test
    public void shouldStillSkipGeneratedLambdaClass() {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        Assume.assumeTrue(instrumentation.isRetransformClassesSupported());
        Class<?> lambdaClass = ((Runnable) () -> { }).getClass();
        final AtomicReference<Class<?>> transformed = new AtomicReference<Class<?>>();
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                transformed.set(classBeingRedefined);
                return null;
            }
        };

        InstrumentationUtils.retransformClasses(instrumentation, transformer,
                Collections.<Class<?>>singleton(lambdaClass));

        assertThat(lambdaClass.isSynthetic()).isTrue();
        assertThat(lambdaClass.getName()).contains("$$Lambda");
        assertThat(transformed.get()).isNull();
    }

    static final class Ordinary$$Lambda$1 implements Runnable {
        @Override
        public void run() {
        }
    }
}
