package com.taobao.arthas.core.util;

import com.taobao.arthas.core.util.ObjectUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class
ObjectUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void containsConstantInput0NotNullFalseOutputFalse() {

    // Arrange
    final Enum[] enumValues = {};

    // Act and Assert result
    Assert.assertFalse(ObjectUtils.containsConstant(enumValues, "A1B2C3", false));
  }

  // Test written by Diffblue Cover.
  @Test
  public void containsConstantInput0NotNullOutputFalse() {

    // Arrange
    final Enum[] enumValues = {};

    // Act and Assert result
    Assert.assertFalse(ObjectUtils.containsConstant(enumValues, "A1B2C3"));
  }

  // Test written by Diffblue Cover.

  @Test
  public void containsElementInput0ZeroOutputFalse() {

    // Arrange
    final Object[] array = {};
    final Object element = 0;

    // Act
    final boolean actual = ObjectUtils.containsElement(array, element);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void getDisplayStringInputNegativeOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("-10000000", ObjectUtils.getDisplayString(-10000000));
  }

  // Test written by Diffblue Cover.
  @Test
  public void hashCodeInputTrueOutputPositive() {

    // Act and Assert result
    Assert.assertEquals(1231, ObjectUtils.hashCode(true));
  }

  // Test written by Diffblue Cover.
  @Test
  public void hashCodeInputZeroOutputZero() {

    // Act and Assert result
    Assert.assertEquals(0, ObjectUtils.hashCode(0.0));
  }

  // Test written by Diffblue Cover.
  @Test
  public void hashCodeInputZeroOutputZero2() {

    // Act and Assert result
    Assert.assertEquals(0, ObjectUtils.hashCode(0.0f));
  }

  // Test written by Diffblue Cover.
  @Test
  public void hashCodeInputZeroOutputZero3() {

    // Act and Assert result
    Assert.assertEquals(0, ObjectUtils.hashCode(0L));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isArrayInputZeroOutputFalse() {

    // Act and Assert result
    Assert.assertFalse(ObjectUtils.isArray(0));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeClassNameInputZeroOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("java.lang.Integer", ObjectUtils.nullSafeClassName(0));
  }

  // Test written by Diffblue Cover.

  @Test
  public void nullSafeHashCodeInput0OutputPositive() {

    // Arrange
    final Object[] array = {};

    // Act
    final int actual = ObjectUtils.nullSafeHashCode(array);

    // Assert result
    Assert.assertEquals(7, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive2() {

    // Arrange
    final boolean[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive3() {

    // Arrange
    final byte[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive4() {

    // Arrange
    final char[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive5() {

    // Arrange
    final double[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive6() {

    // Arrange
    final int[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive7() {

    // Arrange
    final long[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive8() {

    // Arrange
    final float[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeHashCodeInput0OutputPositive9() {

    // Arrange
    final short[] array = {};

    // Act and Assert result
    Assert.assertEquals(7, ObjectUtils.nullSafeHashCode(array));
  }

  // Test written by Diffblue Cover.

  @Test
  public void nullSafeToStringInput0OutputNotNull() {

    // Arrange
    final Object[] array = {};

    // Act
    final String actual = ObjectUtils.nullSafeToString(array);

    // Assert result
    Assert.assertEquals("{}", actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull2() {

    // Arrange
    final byte[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull3() {

    // Arrange
    final char[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull4() {

    // Arrange
    final boolean[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull5() {

    // Arrange
    final int[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull6() {

    // Arrange
    final short[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull7() {

    // Arrange
    final long[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull8() {

    // Arrange
    final float[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }

  // Test written by Diffblue Cover.
  @Test
  public void nullSafeToStringInput0OutputNotNull9() {

    // Arrange
    final double[] array = {};

    // Act and Assert result
    Assert.assertEquals("{}", ObjectUtils.nullSafeToString(array));
  }
}
