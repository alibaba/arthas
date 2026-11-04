package com.taobao.arthas.core.shell.system;

/**
 * 
 * @author hengyunabc 2020-05-18
 *
 */
public interface ProcessAware {

    public Process getProcess();

    public void setProcess(Process process);

}
