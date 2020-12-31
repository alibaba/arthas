package com.alibaba.arthas.spring;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * 
 * @author hengyunabc 2020-06-24
 *
 */
public class StringUtilsTest {
	@Test
	public void test() {

		Map<String, String> map = new HashMap<String, String>();
		map.put("telnet-port", "" + 9999);

		map.put("aaa--bbb", "fff");

		map.put("123", "123");
		map.put("123-", "123");
		map.put("123-abc", "123");

		map.put("xxx-", "xxx");

		map = StringUtils.removeDashKey(map);

		Assertions.assertThat(map).containsEntry("telnetPort", "" + 9999);

		Assertions.assertThat(map).containsEntry("aaa-Bbb", "fff");

		Assertions.assertThat(map).containsEntry("123", "123");
		Assertions.assertThat(map).containsEntry("123-", "123");
		Assertions.assertThat(map).containsEntry("123Abc", "123");
		Assertions.assertThat(map).containsEntry("xxx-", "xxx");

	}
}
