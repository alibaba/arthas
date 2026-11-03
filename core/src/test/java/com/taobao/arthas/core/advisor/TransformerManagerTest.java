package com.taobao.arthas.core.advisor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TransformerManagerTest {

    private static final byte[] ORIGINAL = new byte[] { 1, 2, 3 };
    private static final byte[] CHANGED = new byte[] { 4, 5, 6 };

    private TransformerManager transformerManager;
    private ClassFileTransformer retransformCapable;
    private ClassFileTransformer lazy;

    @Before
    public void setUp() {
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        transformerManager = new TransformerManager(instrumentation);

        ArgumentCaptor<ClassFileTransformer> retransformCapableCaptor = ArgumentCaptor
                .forClass(ClassFileTransformer.class);
        Mockito.verify(instrumentation).addTransformer(retransformCapableCaptor.capture(), Mockito.eq(true));
        retransformCapable = retransformCapableCaptor.getValue();

        ArgumentCaptor<ClassFileTransformer> lazyCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        Mockito.verify(instrumentation).addTransformer(lazyCaptor.capture(), Mockito.eq(false));
        lazy = lazyCaptor.getValue();
    }

    @Test
    public void retransformCapableShouldReturnNullWhenNoTransformerIsRegistered() throws Exception {
        Assertions.assertThat(transform(retransformCapable, String.class)).isNull();
    }

    @Test
    public void retransformCapableShouldReturnNullWhenNoTransformerChangesTheClass() throws Exception {
        transformerManager.addRetransformer(new FixedTransformer(null));
        transformerManager.addTransformer(new FixedTransformer(ORIGINAL), false);
        transformerManager.addTransformer(new FixedTransformer(null), true);

        Assertions.assertThat(transform(retransformCapable, String.class)).isNull();
    }

    @Test
    public void retransformCapableShouldReturnTheChangedBytes() throws Exception {
        transformerManager.addRetransformer(new FixedTransformer(null));
        transformerManager.addTransformer(new FixedTransformer(CHANGED), false);

        Assertions.assertThat(transform(retransformCapable, String.class)).isSameAs(CHANGED);
    }

    @Test
    public void lazyShouldReturnNullWhenNoTransformerChangesTheClass() throws Exception {
        Assertions.assertThat(transform(lazy, null)).isNull();

        transformerManager.addLazyTransformer(new FixedTransformer(null));
        Assertions.assertThat(transform(lazy, null)).isNull();
    }

    @Test
    public void lazyShouldReturnTheChangedBytesOnFirstLoad() throws Exception {
        transformerManager.addLazyTransformer(new FixedTransformer(CHANGED));

        Assertions.assertThat(transform(lazy, null)).isSameAs(CHANGED);
    }

    @Test
    public void lazyShouldIgnoreRetransform() throws Exception {
        transformerManager.addLazyTransformer(new FixedTransformer(CHANGED));

        Assertions.assertThat(transform(lazy, String.class)).isNull();
    }

    private static byte[] transform(ClassFileTransformer transformer, Class<?> classBeingRedefined) throws Exception {
        return transformer.transform(null, "demo/Demo", classBeingRedefined, null, ORIGINAL);
    }

    private static class FixedTransformer implements ClassFileTransformer {

        private final byte[] result;

        FixedTransformer(byte[] result) {
            this.result = result;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            return result;
        }
    }
}
