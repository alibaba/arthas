package com.taobao.arthas.common;

/**
 * @project arthas
 * @desc: command is banned
 * @date 2021-05-13 16:40
 * @author lifl
 */
public class CommandBannedException extends RuntimeException{
    
    public CommandBannedException(String s) {
        super(s);
    }
}
