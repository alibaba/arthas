package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.command.internal.StdoutHandler;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StdoutHandlerTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void injectInput0OutputNull() {

    // Arrange
    final ArrayList<CliToken> tokens = new ArrayList<CliToken>();

    // Act and Assert result
    Assert.assertNull(StdoutHandler.inject(tokens));
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseArgsInput0NotNullOutput0() {

    // Arrange
    final ArrayList<CliToken> tokens = new ArrayList<CliToken>();

    // Act
    final List<String> actual = StdoutHandler.parseArgs(tokens, "BAZ");

    // Assert result
    final LinkedList<String> linkedList = new LinkedList<String>();
    Assert.assertEquals(linkedList, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseArgsInput2NotNullOutputNullPointerException() {

    // Arrange
    final ArrayList<CliToken> tokens = new ArrayList<CliToken>();
    tokens.add(null);
    tokens.add(null);

    // Act
    thrown.expect(NullPointerException.class);
    StdoutHandler.parseArgs(tokens, "\ufe3e");

    // The method is not expected to return due to exception thrown
  }
}
