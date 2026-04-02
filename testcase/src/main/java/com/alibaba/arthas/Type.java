package com.alibaba.arthas;

/**
 * 类型枚举
 *
 * <p>用于定义不同的类型状态</p>
 * <ul>
 *   <li>RUN: 运行状态</li>
 *   <li>STOP: 停止状态</li>
 * </ul>
 */
public enum Type {
    /**
     * 运行状态
     */
    RUN,

    /**
     * 停止状态
     */
    STOP
}
