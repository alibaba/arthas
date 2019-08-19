package com.taobao.arthas.core.view;

import com.taobao.arthas.core.view.Ansi.Attribute;
import com.taobao.arthas.core.view.Ansi.Color;
import com.taobao.arthas.core.view.Ansi.Erase;
import com.taobao.arthas.core.view.Ansi;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class AnsiTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void toStringOutputNotNull() {

    // Arrange
    final Ansi ansi = new Ansi();

    // Act and Assert result
    Assert.assertEquals("", ansi.toString());
  }

  // Test written by Diffblue Cover.
  @Test
  public void toStringOutputNotNull2() {

    // Arrange
    final Ansi.Color ansiColor = Ansi.Color.DEFAULT;

    // Act and Assert result
    Assert.assertEquals("DEFAULT", ansiColor.toString());
  }

  // Test written by Diffblue Cover.
  @Test
  public void toStringOutputNotNull3() {

    // Arrange
    final Ansi.Attribute ansiAttribute = Ansi.Attribute.ITALIC;

    // Act and Assert result
    Assert.assertEquals("ITALIC_ON", ansiAttribute.toString());
  }

  // Test written by Diffblue Cover.
  @Test
  public void toStringOutputNotNull4() {

    // Arrange
    final Ansi.Erase ansiErase = Ansi.Erase.ALL;

    // Act and Assert result
    Assert.assertEquals("ALL", ansiErase.toString());
  }

  // Test written by Diffblue Cover.
  @Test
  public void valueOutputPositive() {

    // Arrange
    final Ansi.Color ansiColor = Ansi.Color.DEFAULT;

    // Act and Assert result
    Assert.assertEquals(9, ansiColor.value());
  }
}
