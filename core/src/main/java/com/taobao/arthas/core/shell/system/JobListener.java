package com.taobao.arthas.core.shell.system;

/**
 * Job listener
 * @author gongdewei 2020-03-23
 */
public interface JobListener {

    void onForeground(Job job);

    void onBackground(Job job);

    void onTerminated(Job job);

    void onSuspend(Job job);
}
