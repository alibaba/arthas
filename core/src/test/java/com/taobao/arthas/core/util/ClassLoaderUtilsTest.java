package com.taobao.arthas.core.util;

import com.taobao.arthas.core.util.ClassLoaderUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ClassLoaderUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getClassLoaderInputNullNullOutputNull() {

    // Act and Assert result
    Assert.assertNull(ClassLoaderUtils.getClassLoader(null, null));
  }
}
