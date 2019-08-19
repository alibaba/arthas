package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class InvalidTelnetOptionExceptionTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getMessageOutputNotNull() {

    // Arrange
    final InvalidTelnetOptionException invalidTelnetOptionException =
        new InvalidTelnetOptionException("3", 1);

    // Act and Assert result
    Assert.assertEquals("3: 1", invalidTelnetOptionException.getMessage());
  }
}
