package com.taobao.arthas.client;

import com.taobao.arthas.client.TelnetConsole;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TelnetConsoleTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getBatchFileOutputNull() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertNull(telnetConsole.getBatchFile());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getCommandOutputNull() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertNull(telnetConsole.getCommand());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getheightOutputNull() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertNull(telnetConsole.getheight());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getPortOutputPositive() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertEquals(3658, telnetConsole.getPort());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getTargetIpOutputNotNull() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertEquals("127.0.0.1", telnetConsole.getTargetIp());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getWidthOutputNull() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertNull(telnetConsole.getWidth());
  }

  // Test written by Diffblue Cover.
  @Test
  public void isHelpOutputFalse() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act and Assert result
    Assert.assertFalse(telnetConsole.isHelp());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setBatchFileInputNotNullOutputVoid() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act
    telnetConsole.setBatchFile("3");

    // Assert side effects
    Assert.assertEquals("3", telnetConsole.getBatchFile());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setCommandInputNotNullOutputVoid() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act
    telnetConsole.setCommand("3");

    // Assert side effects
    Assert.assertEquals("3", telnetConsole.getCommand());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setPortInputZeroOutputVoid() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act
    telnetConsole.setPort(0);

    // Assert side effects
    Assert.assertEquals(0, telnetConsole.getPort());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setTargetIpInputNotNullOutputVoid() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act
    telnetConsole.setTargetIp("3");

    // Assert side effects
    Assert.assertEquals("3", telnetConsole.getTargetIp());
  }

  // Test written by Diffblue Cover.
  @Test
  public void setWidthInputZeroOutputVoid() {

    // Arrange
    final TelnetConsole telnetConsole = new TelnetConsole();

    // Act
    telnetConsole.setWidth(0);

    // Assert side effects
    Assert.assertEquals(new Integer(0), telnetConsole.getWidth());
  }
}
