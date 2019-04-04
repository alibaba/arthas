package com.alibaba.arthas.plugin;

/**
 * TODO 要有一个状态流转图 , 插件有 online /offline 状态？？比如拦截流量的，可以打开一段时间，然后再关掉
 *
 * @author hengyunabc 2019-02-27
 *
 */
public enum PluginState {

    NONE, ENABLED, DISABLED, INITING, INITED, STARTING, STARTED, STOPPING, STOPED, ERROR

}
