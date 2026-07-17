package com.taobao.arthas.core.util.metrics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Regression test for RateCounter.rate() returning NaN when count is 0.
 * When no values have been updated, rate() should return 0.0, not NaN.
 */
public class RateCounterTest {

    @Test
    public void testRateWhenNoUpdatesShouldNotBeNaN() {
        RateCounter counter = new RateCounter();
        double rate = counter.rate();
        assertFalse(Double.isNaN(rate), 
            "RateCounter.rate() should not return NaN when no values have been updated, but got: " + rate);
        assertEquals(0.0, rate, 
            "RateCounter.rate() should return 0.0 when no values have been updated, but got: " + rate);
    }

    @Test
    public void testRateAfterOneUpdate() {
        RateCounter counter = new RateCounter();
        counter.update(100);
        double rate = counter.rate();
        
        assertFalse(Double.isNaN(rate), 
            "RateCounter.rate() should not return NaN after an update, but got: " + rate);
        assertEquals(100.0, rate);
    }

    @Test
    public void testRateAfterMultipleUpdates() {
        RateCounter counter = new RateCounter();
        counter.update(100);
        counter.update(200);
        counter.update(300);
        double rate = counter.rate();
        
        assertFalse(Double.isNaN(rate), 
            "RateCounter.rate() should not return NaN after updates, but got: " + rate);
        assertEquals(200.0, rate); // (100+200+300)/3
    }

    @Test
    public void testRateWithCustomSizeWhenNoUpdates() {
        RateCounter counter = new RateCounter(10);
        double rate = counter.rate();
        
        assertFalse(Double.isNaN(rate), 
            "RateCounter.rate() should not return NaN when no values have been updated (size=10), but got: " + rate);
        assertEquals(0.0, rate);
    }
}
