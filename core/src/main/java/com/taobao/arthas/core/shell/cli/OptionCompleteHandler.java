package com.taobao.arthas.core.shell.cli;

/**
 * 
 * @author hengyunabc 2021-04-29
 *
 */
public interface OptionCompleteHandler {
    boolean matchName(String token);

    boolean complete(Completion completion);
}
