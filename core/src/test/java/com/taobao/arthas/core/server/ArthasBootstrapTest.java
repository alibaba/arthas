package com.taobao.arthas.core.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import org.jboss.modules.ModuleClassLoader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.alibaba.bytekit.utils.ReflectionUtils;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.core.bytecode.TestHelper;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.env.ArthasEnvironment;

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
        Field configureField = ArthasBootstrap.class.getDeclaredField("configure");
        configureField.setAccessible(true);
        ReflectionUtils.setField(configureField, arthasBootstrap, configure);

        Field instrumentationField = ArthasBootstrap.class.getDeclaredField("instrumentation");
        instrumentationField.setAccessible(true);
        ReflectionUtils.setField(instrumentationField, arthasBootstrap, instrumentation);

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

    @Test
    public void testConfigLocationNull() throws Exception {
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();
        String location = ArthasBootstrap.reslove(arthasEnvironment, ArthasBootstrap.CONFIG_LOCATION_PROPERTY, null);
        assertThat(location).isEqualTo(null);
    }

    @Test
    public void testConfigLocation() throws Exception {
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        System.setProperty("hhhh", "fff");
        System.setProperty(ArthasBootstrap.CONFIG_LOCATION_PROPERTY, "test${hhhh}");

        String location = ArthasBootstrap.reslove(arthasEnvironment, ArthasBootstrap.CONFIG_LOCATION_PROPERTY, null);
        System.clearProperty("hhhh");
        System.clearProperty(ArthasBootstrap.CONFIG_LOCATION_PROPERTY);

        assertThat(location).isEqualTo("test" + "fff");
    }

    @Test
    public void testConfigNameDefault() throws Exception {
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        String configName = ArthasBootstrap.reslove(arthasEnvironment, ArthasBootstrap.CONFIG_NAME_PROPERTY, "arthas");
        assertThat(configName).isEqualTo("arthas");
    }

    @Test
    public void testConfigName() throws Exception {
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        System.setProperty(ArthasBootstrap.CONFIG_NAME_PROPERTY, "testName");
        String configName = ArthasBootstrap.reslove(arthasEnvironment, ArthasBootstrap.CONFIG_NAME_PROPERTY, "arthas");
        System.clearProperty(ArthasBootstrap.CONFIG_NAME_PROPERTY);
        assertThat(configName).isEqualTo("testName");
    }
}
