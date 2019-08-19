package com.taobao.arthas.core.util;

import com.taobao.arthas.core.util.ClassUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ClassUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getCodeSourceInputNullOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("", ClassUtils.getCodeSource(null));
  }
}
