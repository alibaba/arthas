package com.taobao.arthas.core.shell.system;

/**
 * 进程感知接口。
 *
 * <p>此接口定义了能够感知和管理进程的对象的标准行为。
 * 实现此接口的类可以：
 * <ul>
 *   <li>获取其关联的进程对象</li>
 *   <li>设置或更新其关联的进程对象</li>
 * </ul>
 *
 * <p>此接口主要用于需要与进程进行交互的组件，
 * 例如命令处理器、任务管理器等。通过实现此接口，
 * 这些组件可以方便地访问和操作进程。
 *
 * @author hengyunabc 2020-05-18
 */
public interface ProcessAware {

    /**
     * 获取与此对象关联的进程。
     *
     * @return 关联的进程对象，如果未设置则可能返回 null
     */
    public Process getProcess();

    /**
     * 设置与此对象关联的进程。
     * 通常在初始化时或进程创建时调用。
     *
     * @param process 要关联的进程对象
     */
    public void setProcess(Process process);

}
