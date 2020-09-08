package com.alibaba.arthas.channel.server.api;

/**
 * Http API response state
 *
 * @author gongdewei 2020-03-19
 */
public enum ApiState {
    /**
     * Response is CONTINUOUS, receiving streaming data of async exec job
     */
    CONTINUOUS,

    /**
     * Request processed successfully
     */
    SUCCEEDED,

    /**
     * Request processing interrupt
     */
    INTERRUPTED,

    /**
     * Request processing failed
     */
    FAILED,

    /**
     * Request is refused
     */
    REFUSED
}
