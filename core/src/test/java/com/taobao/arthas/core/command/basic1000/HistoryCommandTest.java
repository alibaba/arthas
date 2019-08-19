package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.basic1000.HistoryCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class HistoryCommandTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void setNumberInputZeroOutputVoid() {

    // Arrange
    final HistoryCommand historyCommand = new HistoryCommand();

    // Act
    historyCommand.setNumber(0);

    // Assert side effects
    Assert.assertEquals(0, historyCommand.n);
  }
}
