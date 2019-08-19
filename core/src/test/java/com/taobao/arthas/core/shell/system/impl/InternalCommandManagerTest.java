package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class InternalCommandManagerTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void constructorInput0OutputVoid() {

    // Arrange
    final CommandResolver[] resolvers = {};

    // Act, creating object to test constructor
    final InternalCommandManager internalCommandManager = new InternalCommandManager(resolvers);

    // Assert side effects
    final ArrayList<CommandResolver> arrayList = new ArrayList<CommandResolver>();
    Assert.assertEquals(arrayList, internalCommandManager.getResolvers());
  }

  // Test written by Diffblue Cover.
  @Test
  public void getCommandInputNotNullOutputNull() {

    // Arrange
    final ArrayList<CommandResolver> arrayList = new ArrayList<CommandResolver>();
    final InternalCommandManager internalCommandManager = new InternalCommandManager(arrayList);

    // Act and Assert result
    Assert.assertNull(internalCommandManager.getCommand("3"));
  }

  // Test written by Diffblue Cover.
  @Test
  public void getResolversOutput0() {

    // Arrange
    final ArrayList<CommandResolver> arrayList = new ArrayList<CommandResolver>();
    final InternalCommandManager internalCommandManager = new InternalCommandManager(arrayList);

    // Act
    final List<CommandResolver> actual = internalCommandManager.getResolvers();

    // Assert result
    final ArrayList<CommandResolver> arrayList1 = new ArrayList<CommandResolver>();
    Assert.assertEquals(arrayList1, actual);
  }
}
