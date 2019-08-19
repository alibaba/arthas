package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.TelnetCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TelnetCommandTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getCommandInputPositiveOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("EL", TelnetCommand.getCommand(248));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isValidCommandInputPositiveOutputTrue() {

    // Act and Assert result
    Assert.assertTrue(TelnetCommand.isValidCommand(236));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isValidCommandInputZeroOutputFalse() {

    // Act and Assert result
    Assert.assertFalse(TelnetCommand.isValidCommand(0));
  }
}
