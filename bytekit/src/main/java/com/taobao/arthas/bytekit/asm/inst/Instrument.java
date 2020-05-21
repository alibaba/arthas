package com.taobao.arthas.bytekit.asm.inst;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * 在这里支持配置一个 error hander ？做为插入的异常处理的
 *
 *  按名字匹配，按模糊匹配？？有没有这样子的需求？，按interface匹配，按基础类继承的匹配
 *
 *  函数的匹配，直接是名字一样，desc 一样的。 匹配有 annotation 的
 *
 *  只有 NewField 才是加新的field，原来类里有的field，就直接写上就可以了。
 * @author hengyunabc
 *
 */
@Target({ java.lang.annotation.ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrument {
//	Instrumentation
//    MatchType type() default MatchType.ExactClass;

    String[] Class() default {};
    String[] BaseClass() default {};
    String[] Interface() default {};

    String originalName() default "";

    Class<? extends Throwable> suppress() default Throwable.class;

    Class<?> suppressHandler() default Void.class;
}