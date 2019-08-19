package com.taobao.arthas.core.util;

import com.taobao.arthas.core.util.ArthasBanner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ArthasBannerTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  // Test written by Diffblue Cover.
  @Test
  public void tutorialsOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("https://alibaba.github.io/arthas/arthas-tutorials",
                        ArthasBanner.tutorials());
  }

  // Test written by Diffblue Cover.
  @Test
  public void wikiOutputNotNull() {

    // Act and Assert result
    Assert.assertEquals("https://alibaba.github.io/arthas", ArthasBanner.wiki());
  }
}
