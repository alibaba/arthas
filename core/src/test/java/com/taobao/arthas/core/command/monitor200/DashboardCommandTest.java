package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.monitor200.DashboardCommand.MemoryEntry;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class DashboardCommandTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputNotNullPositiveZeroZeroOutputVoid() {

    // Arrange
    final String name = "BAZ";
    final long used = 1048576L;
    final long total = 0L;
    final long max = 0L;

    // Act, creating object to test constructor
    final MemoryEntry dashboardCommandMemoryEntry = new MemoryEntry(name, used, total, max);

    // Assert side effects
    Assert.assertEquals("BAZ", dashboardCommandMemoryEntry.name);
    Assert.assertEquals(1048576, dashboardCommandMemoryEntry.unit);
    Assert.assertEquals("M", dashboardCommandMemoryEntry.unitStr);
    Assert.assertEquals(1048576L, dashboardCommandMemoryEntry.used);
  }

  // Test written by Diffblue Cover.

  @Test
  public void constructorInputNotNullZeroZeroZeroOutputVoid() {

    // Arrange
    final String name = "BAZ";
    final long used = 0L;
    final long total = 0L;
    final long max = 0L;

    // Act, creating object to test constructor
    final MemoryEntry dashboardCommandMemoryEntry = new MemoryEntry(name, used, total, max);

    // Assert side effects
    Assert.assertEquals("BAZ", dashboardCommandMemoryEntry.name);
    Assert.assertEquals(1024, dashboardCommandMemoryEntry.unit);
    Assert.assertEquals("K", dashboardCommandMemoryEntry.unitStr);
  }
}
