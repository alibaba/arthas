package com.taobao.arthas.core.view;

import com.taobao.arthas.core.view.LadderView;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class LadderViewTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void drawOutputNotNull() {

    // Arrange
    final LadderView ladderView = new LadderView();

    // Act and Assert result
    Assert.assertEquals("", ladderView.draw());
  }
}
