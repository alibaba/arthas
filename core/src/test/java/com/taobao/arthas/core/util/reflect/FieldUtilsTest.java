package com.taobao.arthas.core.util.reflect;

import com.taobao.arthas.core.util.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class FieldUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void isPackageAccessInputPositiveOutputFalse() {

    // Act and Assert result
    Assert.assertFalse(FieldUtils.isPackageAccess(1));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isPackageAccessInputZeroOutputTrue() {

    // Act and Assert result
    Assert.assertTrue(FieldUtils.isPackageAccess(0));
  }

  // Test written by Diffblue Cover.

  @Test
  public void isTrueInputFalseNotNull0OutputIllegalArgumentException() {

    // Arrange
    final boolean expression = false;
    final String message = "3";
    final Object[] values = {};

    // Act
    thrown.expect(IllegalArgumentException.class);
    FieldUtils.isTrue(expression, message, values);

    // The method is not expected to return due to exception thrown
  }
}
