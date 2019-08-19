package com.taobao.arthas.core.advisor;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class AdviceWeaverTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void regInputZeroNullOutputNullPointerException() {

    // Arrange
    final int adviceId = 0;
    final AdviceListener listener = null;

    // Act
    thrown.expect(NullPointerException.class);
    AdviceWeaver.reg(adviceId, listener);

    // The method is not expected to return due to exception thrown
  }
}
