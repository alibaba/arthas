package com.taobao.arthas.core.env;

import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * 
 * @author hengyunabc 2019-12-27
 *
 */
public class ArthasEnvironmentTest {

    @Test
    public void test() {
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        Assertions.assertThat(arthasEnvironment.resolvePlaceholders("hello, ${java.version}"))
                .isEqualTo("hello, " + System.getProperty("java.version"));

        Assertions.assertThat(arthasEnvironment.resolvePlaceholders("hello, ${xxxxxxxxxxxxxxx}"))
                .isEqualTo("hello, ${xxxxxxxxxxxxxxx}");

        System.setProperty("xxxxxxxxxxxxxxx", "vvv");

        Assertions.assertThat(arthasEnvironment.resolvePlaceholders("hello, ${xxxxxxxxxxxxxxx}"))
                .isEqualTo("hello, vvv");

        System.clearProperty("xxxxxxxxxxxxxxx");
    }

    @Test
    public void test_properties() {
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        Properties properties1 = new Properties();
        Properties properties2 = new Properties();
        arthasEnvironment.addLast(new PropertiesPropertySource("test1", properties1));
        arthasEnvironment.addLast(new PropertiesPropertySource("test2", properties2));

        properties2.put("test.key", "2222");

        Assertions.assertThat(arthasEnvironment.resolvePlaceholders("hello, ${test.key}")).isEqualTo("hello, 2222");

        properties1.put("java.version", "test");
        properties1.put("test.key", "test");

        Assertions.assertThat(arthasEnvironment.resolvePlaceholders("hello, ${java.version}"))
                .isEqualTo("hello, " + System.getProperty("java.version"));

        Assertions.assertThat(arthasEnvironment.resolvePlaceholders("hello, ${test.key}")).isEqualTo("hello, test");
    }
}
