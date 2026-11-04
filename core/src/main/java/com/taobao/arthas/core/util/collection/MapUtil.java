package com.taobao.arthas.core.util.collection;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Ares
 * @time: 2025-09-25 12:34:07
 * @description: Map util
 * @version: JDK 1.8
 */
public class MapUtil {

  private static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

  /**
   * @see java.util.HashMap#DEFAULT_INITIAL_CAPACITY
   */
  private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

  public static int capacity(int expectedSize) {
    if (expectedSize < 0) {
      return DEFAULT_INITIAL_CAPACITY;
    }

    if (expectedSize < 3) {
      return expectedSize + 1;
    }
    if (expectedSize < MAX_POWER_OF_TWO) {
      // This is the calculation used in JDK8 to resize when a putAll
      // happens; it seems to be the most conservative calculation we
      // can make.  0.75 is the default load factor.
      return (int) ((float) expectedSize / 0.75F + 1.0F);
    }
    return Integer.MAX_VALUE;
  }

  public static <K, V> Map<K, V> newHashMap(int expectedSize) {
    return new HashMap<>(capacity(expectedSize));
  }

}
