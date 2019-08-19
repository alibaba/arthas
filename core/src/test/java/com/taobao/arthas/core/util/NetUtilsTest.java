package com.taobao.arthas.core.util;

import com.taobao.arthas.core.util.NetUtils.Response;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class NetUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getContentOutputNotNull() {

    // Arrange
    final Response netUtilsResponse = new Response("3");

    // Act and Assert result
    Assert.assertEquals("3", netUtilsResponse.getContent());
  }

  // Test written by Diffblue Cover.
  @Test
  public void isSuccessOutputTrue() {

    // Arrange
    final Response netUtilsResponse = new Response("foo");

    // Act and Assert result
    Assert.assertTrue(netUtilsResponse.isSuccess());
  }
}
