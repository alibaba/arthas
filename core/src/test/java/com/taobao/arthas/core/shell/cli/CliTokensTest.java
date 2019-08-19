package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.util.LinkedList;
import java.util.List;

public class CliTokensTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void tokenizeInputNotNullOutput0() {

    // Arrange
    final String s = "";

    // Act
    final List<CliToken> actual = CliTokens.tokenize(s);

    // Assert result
    final LinkedList<CliToken> linkedList = new LinkedList<CliToken>();
    Assert.assertEquals(linkedList, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void tokenizeInputNullOutputNullPointerException() {

    // Arrange
    final String s = null;

    // Act
    thrown.expect(NullPointerException.class);
    CliTokens.tokenize(s);

    // The method is not expected to return due to exception thrown
  }
}
