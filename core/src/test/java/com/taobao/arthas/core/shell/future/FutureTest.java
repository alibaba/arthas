package com.taobao.arthas.core.shell.future;

import com.taobao.arthas.core.shell.future.Future;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class FutureTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void causeOutputNull() {

    // Arrange
    final Future future = new Future();

    // Act and Assert result
    Assert.assertNull(future.cause());
  }

  // Test written by Diffblue Cover.

  @Test
  public void failedOutputFalse() {

    // Arrange
    final Future future = new Future();

    // Act and Assert result
    Assert.assertFalse(future.failed());
  }

  // Test written by Diffblue Cover.
  @Test
  public void isCompleteOutputFalse() {

    // Arrange
    final Future future = new Future();

    // Act and Assert result
    Assert.assertFalse(future.isComplete());
  }

  // Test written by Diffblue Cover.
  @Test
  public void resultOutputNull() {

    // Arrange
    final Future future = new Future();

    // Act and Assert result
    Assert.assertNull(future.result());
  }

  // Test written by Diffblue Cover.
  @Test
  public void succeededOutputFalse() {

    // Arrange
    final Future future = new Future();

    // Act and Assert result
    Assert.assertFalse(future.succeeded());
  }
}
