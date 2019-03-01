package com.taobao.arthas.plugin;

/**
 *
 * @author hengyunabc 2019-03-01
 *
 */
public class PluginException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PluginException() {
        super();
    }

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
