package com.taobao.arthas.core.view;

import com.taobao.arthas.core.view.TableView.ColumnDefine;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class TableViewTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void getHighOutputZero() {

    // Arrange
    final ColumnDefine tableViewColumnDefine = new ColumnDefine();

    // Act and Assert result
    Assert.assertEquals(0, tableViewColumnDefine.getHigh());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getWidthOutputZero() {

    // Arrange
    final ColumnDefine tableViewColumnDefine = new ColumnDefine();

    // Act and Assert result
    Assert.assertEquals(0, tableViewColumnDefine.getWidth());
  }
}
