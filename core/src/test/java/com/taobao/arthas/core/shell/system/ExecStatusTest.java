package com.taobao.arthas.core.shell.system;

import com.taobao.arthas.core.shell.system.ExecStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ExecStatusTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void valueOfInputNotNullOutputIllegalArgumentException() {

    // Act
    thrown.expect(IllegalArgumentException.class);
    ExecStatus.valueOf("3");

    // The method is not expected to return due to exception thrown
  }
}
