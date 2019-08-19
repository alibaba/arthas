package com.taobao.arthas.core.config;

import com.taobao.arthas.core.config.FeatureCodec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class FeatureCodecTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputNotNullNotNullOutputIllegalArgumentException() {

    // Arrange
    final char kvSegmentSeparator = '\\';
    final char kvSeparator = '!';

    // Act, creating object to test constructor
    thrown.expect(IllegalArgumentException.class);
    final FeatureCodec featureCodec = new FeatureCodec(kvSegmentSeparator, kvSeparator);

    // The method is not expected to return due to exception thrown
  }
}
