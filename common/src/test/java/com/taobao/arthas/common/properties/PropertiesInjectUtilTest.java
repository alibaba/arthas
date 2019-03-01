package com.taobao.arthas.common.properties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.arthas.common.properties.ErrorProperties.IncludeStacktrace;


public class PropertiesInjectUtilTest {

	@Test
	public void test() throws UnknownHostException {
		Properties p = new Properties();
		p.put("server.port", "8080");

		p.put("server.host", "localhost");
		p.put("server.flag", "true");

		p.put("server.address", "192.168.1.1");

		p.put("server.ssl.enabled", "true");
		p.put("server.ssl.protocol", "TLS");
		p.put("server.ssl.testBoolean", "false");
		p.put("server.ssl.testLong", "123");
		p.put("server.ssl.ciphers", "abc, efg ,hij");

		p.put("server.error.includeStacktrace", "ALWAYS");

		Server server = new Server();

		PropertiesInjectUtil.inject(p, server);

		System.out.println(server);

		Assert.assertEquals(server.getPort(), 8080);
		Assert.assertEquals(server.getHost(), "localhost");
		Assert.assertTrue(server.isFlag());

		Assert.assertEquals(server.getAddress(), InetAddress.getByName("192.168.1.1"));

		Assert.assertEquals(server.ssl.getProtocol(), "TLS");
		Assert.assertTrue(server.ssl.isEnabled());
		Assert.assertFalse(server.ssl.getTestBoolean());
		Assert.assertEquals(server.ssl.getTestLong(), Long.valueOf(123));
		Assert.assertNull(server.ssl.getTestDouble());

		Assert.assertArrayEquals(server.ssl.getCiphers(), new String[] { "abc", "efg", "hij" });

		Assert.assertEquals(server.error.getIncludeStacktrace(), IncludeStacktrace.ALWAYS);

	}

}
