package com.taobao.arthas.core.util.collection;

import com.taobao.arthas.core.util.collection.ThreadUnsafeFixGaStack;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.util.NoSuchElementException;

public class ThreadUnsafeFixGaStackTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void isEmptyOutputTrue() {

    // Arrange
    final ThreadUnsafeFixGaStack threadUnsafeFixGaStack = new ThreadUnsafeFixGaStack(0);

    // Act and Assert result
    Assert.assertTrue(threadUnsafeFixGaStack.isEmpty());
  }

  // Test written by Diffblue Cover.
  @Test
  public void peekOutputNoSuchElementException() {

    // Arrange
    final ThreadUnsafeFixGaStack threadUnsafeFixGaStack = new ThreadUnsafeFixGaStack(0);

    // Act
    thrown.expect(NoSuchElementException.class);
    threadUnsafeFixGaStack.peek();

    // The method is not expected to return due to exception thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void popOutputNoSuchElementException() {

    // Arrange
    final ThreadUnsafeFixGaStack threadUnsafeFixGaStack = new ThreadUnsafeFixGaStack(0);

    // Act
    thrown.expect(NoSuchElementException.class);
    threadUnsafeFixGaStack.pop();

    // The method is not expected to return due to exception thrown
  }
}
