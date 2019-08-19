package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.TelnetOption;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TelnetOptionTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getOptionInputPositiveOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("UNASSIGNED", TelnetOption.getOption(213));
  }

  // Test written by Diffblue Cover.
  @Test
  public void getOptionInputPositiveOutputNotNull2() {

    // Act and Assert result
    Assert.assertEquals("SUPDUP", TelnetOption.getOption(21));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isValidOptionInputNegativeOutputTrue() {

    // Act and Assert result
    Assert.assertTrue(TelnetOption.isValidOption(-2147483391));
  }

  // Test written by Diffblue Cover.
  @Test
  public void isValidOptionInputPositiveOutputFalse() {

    // Act and Assert result
    Assert.assertFalse(TelnetOption.isValidOption(257));
  }
}
