package com.taobao.arthas.common;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the configurable tunnel-client max HTTP content length introduced
 * for issue #3034 ("Unable to download JFR recordings larger than 10MB via
 * Tunnel Server").
 */
public class ArthasConstantsTest {

    private static final String PROPERTY = ArthasConstants.TUNNEL_CLIENT_MAX_HTTP_CONTENT_LENGTH_PROPERTY;

    private String originalValue;

    @Before
    public void saveProperty() {
        originalValue = System.getProperty(PROPERTY);
        System.clearProperty(PROPERTY);
    }

    @After
    public void restoreProperty() {
        if (originalValue == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, originalValue);
        }
    }

    @Test
    public void defaultsToTenMegabytesWhenPropertyMissing() {
        assertEquals(ArthasConstants.MAX_HTTP_CONTENT_LENGTH,
                ArthasConstants.getTunnelClientMaxHttpContentLength());
    }

    @Test
    public void honoursConfiguredPropertyValue() {
        int oneHundredMb = 100 * 1024 * 1024;
        System.setProperty(PROPERTY, Integer.toString(oneHundredMb));

        assertEquals(oneHundredMb, ArthasConstants.getTunnelClientMaxHttpContentLength());
    }

    @Test
    public void fallsBackToDefaultWhenPropertyIsNotANumber() {
        System.setProperty(PROPERTY, "not-a-number");

        assertEquals(ArthasConstants.MAX_HTTP_CONTENT_LENGTH,
                ArthasConstants.getTunnelClientMaxHttpContentLength());
    }

    @Test
    public void fallsBackToDefaultWhenPropertyIsNotPositive() {
        System.setProperty(PROPERTY, "0");
        assertEquals(ArthasConstants.MAX_HTTP_CONTENT_LENGTH,
                ArthasConstants.getTunnelClientMaxHttpContentLength());

        System.setProperty(PROPERTY, "-1");
        assertEquals(ArthasConstants.MAX_HTTP_CONTENT_LENGTH,
                ArthasConstants.getTunnelClientMaxHttpContentLength());
    }
}
