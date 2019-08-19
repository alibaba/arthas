package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TerminalTypeOptionHandlerTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void answerSubnegotiationInput0ZeroOutputNull() {

    // Arrange
    final TerminalTypeOptionHandler terminalTypeOptionHandler = new TerminalTypeOptionHandler("3");
    final int[] suboptionData = {};

    // Act and Assert result
    Assert.assertNull(terminalTypeOptionHandler.answerSubnegotiation(suboptionData, 0));
  }

  // Test written by Diffblue Cover.
  @Test
  public void answerSubnegotiationInput8PositiveOutputNull() {

    // Arrange
    final TerminalTypeOptionHandler terminalTypeOptionHandler = new TerminalTypeOptionHandler("3");
    final int[] suboptionData = {0, 0, 0, 0, 0, 0, 0, 0};

    // Act and Assert result
    Assert.assertNull(terminalTypeOptionHandler.answerSubnegotiation(suboptionData, 2));
  }

  // Test written by Diffblue Cover.
  @Test
  public void answerSubnegotiationInput8PositiveOutputNull2() {

    // Arrange
    final TerminalTypeOptionHandler terminalTypeOptionHandler = new TerminalTypeOptionHandler("3");
    final int[] suboptionData = {24, 0, 0, 0, 0, 0, 0, 0};

    // Act and Assert result
    Assert.assertNull(terminalTypeOptionHandler.answerSubnegotiation(suboptionData, 2));
  }
}
