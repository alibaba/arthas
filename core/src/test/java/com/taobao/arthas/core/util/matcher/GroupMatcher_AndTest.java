package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.util.matcher.GroupMatcher.And;
import com.taobao.arthas.core.util.matcher.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;

public class GroupMatcher_AndTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void addInputNotNullOutputUnsupportedOperationException() {

    // Arrange
    final Matcher[] matcherArray = {};
    final And groupMatcherAnd = new And(matcherArray);
    final Matcher[] matcherArray1 = {};
    final And matcher = new And(matcherArray1);

    // Act
    thrown.expect(UnsupportedOperationException.class);
    groupMatcherAnd.add(matcher);

    // The method is not expected to return due to exception thrown
  }
}
