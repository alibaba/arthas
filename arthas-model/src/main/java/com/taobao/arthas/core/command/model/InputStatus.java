package com.taobao.arthas.core.command.model;

/**
 * Command input status for webui
 * @author gongdewei 2020/4/14
 */
public enum InputStatus {
    /**
     * Allow input new commands
     */
    ALLOW_INPUT,

    /**
     * Allow interrupt running job
     */
    ALLOW_INTERRUPT,

    /**
     * Disable input and interrupt
     */
    DISABLED
}
