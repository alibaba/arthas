package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.monitor200.MBeanCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class MBeanCommandTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getIntervalOutputZero() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act and Assert result
    Assert.assertEquals(0L, mBeanCommand.getInterval());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getNameOutputNull() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act and Assert result
    Assert.assertNull(mBeanCommand.getName());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getNumOfExecutionsOutputPositive() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act and Assert result
    Assert.assertEquals(100, mBeanCommand.getNumOfExecutions());
  }

  // Test written by Diffblue Cover.
  @Test
  public void isMetaDataOutputFalse() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act and Assert result
    Assert.assertFalse(mBeanCommand.isMetaData());
  }

  // Test written by Diffblue Cover.
  @Test
  public void isRegExOutputFalse() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act and Assert result
    Assert.assertFalse(mBeanCommand.isRegEx());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setNamePatternInputNotNullOutputVoid() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act
    mBeanCommand.setNamePattern("3");

    // Assert side effects
    Assert.assertEquals("3", mBeanCommand.getName());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setNumOfExecutionsInputZeroOutputVoid() {

    // Arrange
    final MBeanCommand mBeanCommand = new MBeanCommand();

    // Act
    mBeanCommand.setNumOfExecutions(0);

    // Assert side effects
    Assert.assertEquals(0, mBeanCommand.getNumOfExecutions());
  }
}
