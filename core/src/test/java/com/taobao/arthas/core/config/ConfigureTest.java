package com.taobao.arthas.core.config;

import com.taobao.arthas.core.config.Configure;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ConfigureTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getArthasAgentOutputNull() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertNull(configure.getArthasAgent());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getArthasCoreOutputNull() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertNull(configure.getArthasCore());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getHttpPortOutputZero() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertEquals(0, configure.getHttpPort());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getIpOutputNull() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertNull(configure.getIp());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getJavaPidOutputZero() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertEquals(0, configure.getJavaPid());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getSessionTimeoutOutputPositive() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertEquals(1800L, configure.getSessionTimeout());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getTelnetPortOutputZero() {

    // Arrange
    final Configure configure = new Configure();

    // Act and Assert result
    Assert.assertEquals(0, configure.getTelnetPort());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setArthasAgentInputNotNullOutputVoid() {

    // Arrange
    final Configure configure = new Configure();

    // Act
    configure.setArthasAgent("1");

    // Assert side effects
    Assert.assertEquals("1", configure.getArthasAgent());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setArthasCoreInputNotNullOutputVoid() {

    // Arrange
    final Configure configure = new Configure();

    // Act
    configure.setArthasCore("1");

    // Assert side effects
    Assert.assertEquals("1", configure.getArthasCore());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setIpInputNotNullOutputVoid() {

    // Arrange
    final Configure configure = new Configure();

    // Act
    configure.setIp("1");

    // Assert side effects
    Assert.assertEquals("1", configure.getIp());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setSessionTimeoutInputZeroOutputVoid() {

    // Arrange
    final Configure configure = new Configure();

    // Act
    configure.setSessionTimeout(0L);

    // Assert side effects
    Assert.assertEquals(0L, configure.getSessionTimeout());
  }
}
