package com.taobao.arthas.core.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.instrument.Instrumentation;

import org.jboss.modules.ModuleClassLoader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.core.bytecode.TestHelper;
import com.taobao.arthas.core.config.Configure;

import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * 
 * @author hengyunabc 2020-12-02
 *
 */
public class ArthasBootstrapTest {
    @Before
    public void beforeMethod() {
        // jboss modules need jdk8
        org.junit.Assume.assumeTrue(JavaVersionUtils.isGreaterThanJava7());
    }

    @Test
    public void test() throws Exception {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        TestHelper.appendSpyJar(instrumentation);

        ArthasBootstrap arthasBootstrap = Mockito.mock(ArthasBootstrap.class);
        Mockito.doCallRealMethod().when(arthasBootstrap).enhanceClassLoader();

        Configure configure = Mockito.mock(Configure.class);
        Mockito.when(configure.getEnhanceLoaders())
                .thenReturn("java.lang.ClassLoader,org.jboss.modules.ConcurrentClassLoader");
        FieldSetter.setField(arthasBootstrap, ArthasBootstrap.class.getDeclaredField("configure"), configure);
        FieldSetter.setField(arthasBootstrap, ArthasBootstrap.class.getDeclaredField("instrumentation"),
                instrumentation);

        org.jboss.modules.ModuleClassLoader moduleClassLoader = Mockito.mock(ModuleClassLoader.class);

        boolean flag = false;
        try {
            moduleClassLoader.loadClass("java.arthas.SpyAPI");
        } catch (Exception e) {
            flag = true;
        }
        assertThat(flag).isTrue();

        arthasBootstrap.enhanceClassLoader();

        Class<?> loadClass = moduleClassLoader.loadClass("java.arthas.SpyAPI");

        System.err.println(loadClass);

    }
}
