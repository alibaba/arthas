package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.core.shell.term.TermServer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TermServerTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void createHttpTermServerOutputNull() {

    // Act and Assert result
    Assert.assertNull(TermServer.createHttpTermServer());
  }
}
