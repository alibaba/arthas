package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.command.basic1000.CatCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class AnnotatedCommandTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void cliOutputNull() {

    // Arrange
    final CatCommand annotatedCommand = new CatCommand();

    // Act and Assert result
    Assert.assertNull(annotatedCommand.cli());
  }

  // Test written by Diffblue Cover.
  @Test
  public void nameOutputNull() {

    // Arrange
    final CatCommand annotatedCommand = new CatCommand();

    // Act and Assert result
    Assert.assertNull(annotatedCommand.name());
  }
}
