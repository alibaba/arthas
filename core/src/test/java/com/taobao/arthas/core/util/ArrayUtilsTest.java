package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author earayu
 */
public class ArrayUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmptyLongArray() {
        Assert.assertArrayEquals(ArrayUtils.EMPTY_LONG_ARRAY, new long[0]);
    }

    @Test
    public void testToPrimitive() {
        Assert.assertArrayEquals(ArrayUtils.toPrimitive(null), null);
        Assert.assertArrayEquals(ArrayUtils.toPrimitive(new Long[0]), new long[0]);
        Assert.assertArrayEquals(
                ArrayUtils.toPrimitive(new Long[]{
                        1L,
                        1763L,
                        54769975464L
                }),
                new long[]{
                        1L,
                        1763L,
                        54769975464L
                });
        //throws NullPointerException if array content is null
        thrown.expect(NullPointerException.class);
        Assert.assertArrayEquals(ArrayUtils.toPrimitive(new Long[]{null}), new long[]{1L});
    }
}
