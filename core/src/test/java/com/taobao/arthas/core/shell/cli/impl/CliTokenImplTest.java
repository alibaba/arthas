package com.taobao.arthas.core.shell.cli.impl;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class CliTokenImplTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void equalsInputNotNullOutputFalse() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(true, "3");
    final CliTokenImpl obj = new CliTokenImpl(false, "3");

    // Act and Assert result
    Assert.assertFalse(cliTokenImpl.equals(obj));
  }

  // Test written by Diffblue Cover.
  @Test
  public void equalsInputNotNullOutputFalse2() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(false, "3");
    final CliTokenImpl obj = new CliTokenImpl(false, "a\'b\'c");

    // Act and Assert result
    Assert.assertFalse(cliTokenImpl.equals(obj));
  }

  // Test written by Diffblue Cover.
  @Test
  public void equalsInputNotNullOutputTrue() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(false, "3");
    final CliTokenImpl obj = new CliTokenImpl(false, "3");

    // Act and Assert result
    Assert.assertTrue(cliTokenImpl.equals(obj));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isBlankOutputFalse() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(true, ",");

    // Act and Assert result
    Assert.assertFalse(cliTokenImpl.isBlank());
  }

  // Test written by Diffblue Cover.
  @Test
  public void isBlankOutputTrue() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(false, ",");

    // Act and Assert result
    Assert.assertTrue(cliTokenImpl.isBlank());
  }

  // Test written by Diffblue Cover.
  @Test
  public void rawOutputNotNull() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(false, "1");

    // Act and Assert result
    Assert.assertEquals("1", cliTokenImpl.raw());
  }

  // Test written by Diffblue Cover.

  @Test
  public void tokenizeInputNullOutputNullPointerException() {

    // Arrange
    final String s = null;

    // Act
    thrown.expect(NullPointerException.class);
    CliTokenImpl.tokenize(s);

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void toStringOutputNotNull() {

    // Arrange
    final CliTokenImpl cliTokenImpl = new CliTokenImpl(true, "\'");

    // Act and Assert result
    Assert.assertEquals("CliToken[text=true,value=\']", cliTokenImpl.toString());
  }
}
