package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.cli.CompletionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class CompletionUtilsTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void completeShortOptionInputNullNullNullOutputNullPointerException() {

    // Act
    thrown.expect(NullPointerException.class);
    CompletionUtils.completeShortOption(null, null, null);

    // The method is not expected to return due to exception thrown
  }
}
