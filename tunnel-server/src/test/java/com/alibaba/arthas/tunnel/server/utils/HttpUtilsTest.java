package com.alibaba.arthas.tunnel.server.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * 
 * @author hengyunabc 2021-02-26
 *
 */
public class HttpUtilsTest {

    @Test
    public void test1() {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(headers.get("X-Forwarded-For")).thenReturn("30.25.233.172, 11.162.179.161");

        String ip = HttpUtils.findClientIP(headers);

        Assertions.assertThat(ip).isEqualTo("30.25.233.172");
    }

    @Test
    public void test2() {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(headers.get("X-Forwarded-For")).thenReturn("30.25.233.172");

        String ip = HttpUtils.findClientIP(headers);

        Assertions.assertThat(ip).isEqualTo("30.25.233.172");

    }

    @Test
    public void test3() {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(headers.get("X-Forwarded-For")).thenReturn(null);

        String ip = HttpUtils.findClientIP(headers);

        Assertions.assertThat(ip).isEqualTo(null);

    }
}
