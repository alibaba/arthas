package com.taobao.arthas.core.shell;

import com.taobao.arthas.core.util.ArthasBanner;

import java.lang.instrument.Instrumentation;

/**
 * The configurations options for the shell server.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellServerOptions {

    /**
     * Default of how often, in ms, to check for expired sessions
     */
    public static final long DEFAULT_REAPER_INTERVAL = 60 * 1000; // 60 seconds

    /**
     * Default time, in ms, that a shell session lasts for without being accessed before expiring.
     */
    public static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    /**
     * Default time, in ms, that a server waits for a client to connect
     */
    public static final long DEFAULT_CONNECTION_TIMEOUT = 6000; // 6 seconds

    public static final String DEFAULT_WELCOME_MESSAGE = ArthasBanner.welcome();

    public static final String DEFAULT_INPUTRC = "com/taobao/arthas/core/shell/term/readline/inputrc";

    private String welcomeMessage;
    private long sessionTimeout;
    private long reaperInterval;
    private long connectionTimeout;
    private long pid;
    private Instrumentation instrumentation;

    public ShellServerOptions() {
        welcomeMessage = DEFAULT_WELCOME_MESSAGE;
        sessionTimeout = DEFAULT_SESSION_TIMEOUT;
        connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        reaperInterval = DEFAULT_REAPER_INTERVAL;
    }

    /**
     * @return the shell welcome message
     */
    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    /**
     * Set the shell welcome message, i.e the message displayed in the user console when he connects to the shell.
     *
     * @param welcomeMessage the welcome message
     * @return a reference to this, so the API can be used fluently
     */
    public ShellServerOptions setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
        return this;
    }

    /**
     * @return the session timeout
     */
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Set the session timeout.
     *
     * @param sessionTimeout the new session timeout
     * @return a reference to this, so the API can be used fluently
     */
    public ShellServerOptions setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    /**
     * @return the reaper interval
     */
    public long getReaperInterval() {
        return reaperInterval;
    }

    /**
     * Set the repear interval, i.e the period at which session eviction is performed.
     *
     * @param reaperInterval the new repeat interval
     * @return a reference to this, so the API can be used fluently
     */
    public ShellServerOptions setReaperInterval(long reaperInterval) {
        this.reaperInterval = reaperInterval;
        return this;
    }

    public ShellServerOptions setPid(long pid) {
        this.pid = pid;
        return this;
    }

    public ShellServerOptions setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        return this;
    }

    public long getPid() {
        return pid;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
