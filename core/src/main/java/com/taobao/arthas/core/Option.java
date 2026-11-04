package com.taobao.arthas.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Arthas全局选项
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {

    /*
     * 选项级别，数字越小级别越高
     */
    int level();

    /*
     * 选项名称
     */
    String name();

    /*
     * 选项摘要说明
     */
    String summary();

    /*
     * 命令描述
     */
    String description();

}