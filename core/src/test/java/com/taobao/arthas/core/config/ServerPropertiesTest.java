package com.taobao.arthas.core.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.arthas.core.config.ErrorProperties.IncludeStacktrace;
import com.taobao.arthas.core.env.ArthasEnvironment;
import com.taobao.arthas.core.env.PropertiesPropertySource;


public class ServerPropertiesTest {

    @Test
    public void test() throws UnknownHostException {
        Properties p = new Properties();
        p.put("server.port", "8080");

        p.put("server.address", "192.168.1.1");

        p.put("server.ssl.enabled", "true");
        p.put("server.ssl.protocol", "TLS");
        p.put("server.ssl.ciphers", "abc, efg ,hij");

        p.put("server.error.includeStacktrace", "ALWAYS");
        
        
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        arthasEnvironment.addLast(new PropertiesPropertySource("test1", p));

        ServerProperties serverProperties = new ServerProperties();

        BinderUtils.inject(arthasEnvironment, serverProperties);

        Assert.assertEquals(serverProperties.getPort().intValue(), 8080);

        Assert.assertEquals(serverProperties.getAddress(), InetAddress.getByName("192.168.1.1"));

        Assert.assertEquals(serverProperties.getSsl().getProtocol(), "TLS");
        Assert.assertTrue(serverProperties.getSsl().isEnabled());

        Assert.assertArrayEquals(serverProperties.getSsl().getCiphers(), new String[] { "abc", "efg", "hij" });

        Assert.assertEquals(serverProperties.getError().getIncludeStacktrace(), IncludeStacktrace.ALWAYS);

    }

    @Test
    public void testSystemProperties() {
        Properties p = new Properties();
        p.put("system.test.systemKey", "kkk");
        p.put("system.test.nonSystemKey", "xxxx");
        p.put("system.test.systemIngeger", "123");

        System.setProperty("system.test.systemKey", "ssss");
        System.setProperty("system.test.systemIngeger", "110");
        
        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();

        arthasEnvironment.addLast(new PropertiesPropertySource("test1", p));

        SystemObject systemObject = new SystemObject();

        BinderUtils.inject(arthasEnvironment, systemObject);

        Assert.assertEquals(systemObject.getSystemKey(), "ssss");
        Assert.assertEquals(systemObject.getNonSystemKey(), "xxxx");
        Assert.assertEquals(systemObject.getSystemIngeger(), 110);

    }

}
