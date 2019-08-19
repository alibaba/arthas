package com.taobao.arthas.core.command.klass100;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;

public class JadCommandTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void retransformClassesInputNullNull1OutputNullPointerException() {

    // Arrange
    final Instrumentation inst = null;
    final ClassFileTransformer transformer = null;
    final HashSet classes = new HashSet();
    classes.add(null);

    // Act
    thrown.expect(NullPointerException.class);
    JadCommand.retransformClasses(inst, transformer, classes);

    // The method is not expected to return due to exception thrown
  }
}
