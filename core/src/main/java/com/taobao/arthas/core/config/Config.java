package com.taobao.arthas.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置注解
 * <p>
 * 用于标记配置类，指定配置项的前缀。当从命令行、配置文件等来源读取配置时，
 * 会根据此前缀来匹配和解析相关的配置参数。
 * 例如，如果prefix为"arthas"，则配置项"arthas.ip"会被映射到被注解类的ip字段。
 *
 * @author hengyunabc 2019-08-05
 */
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，可以通过反射读取
@Target(ElementType.TYPE) // 此注解只能用于类、接口或枚举类型上
public @interface Config {

    /**
     * 配置项的前缀
     * <p>
     * 用于指定配置项名称的前缀，默认为空字符串。
     * 例如：prefix设置为"arthas"时，配置项"arthas.ip"会映射到注解类的ip字段。
     * 如果配置文件中有"arthas.ip=127.0.0.1"，则会自动解析并设置到对应字段。
     *
     * @return 配置前缀字符串
     */
    String prefix() default "";

}