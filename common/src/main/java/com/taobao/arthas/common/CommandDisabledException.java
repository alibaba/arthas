package com.taobao.arthas.common;

/**
 * @project arthas
 * @desc: command is disabled
 * @date 2021-05-13 16:40
 * @author lifl
 */
public class CommandDisabledException extends RuntimeException{
    
    public CommandDisabledException(String s) {
        super(s);
    }
}
