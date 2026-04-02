package com.taobao.arthas.core.util;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 * Instrumentation 工具类
 * 提供 Java Instrumentation API 相关的工具方法，用于类的重转换（Retransform）
 * 主要用于 Arthas 的字节码增强功能
 *
 * @author hengyunabc 2020-05-25
 *
 */
public class InstrumentationUtils {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(InstrumentationUtils.class);

    /**
     * 重转换类集合
     * 使用提供的转换器对指定的类集合进行字节码重转换
     * 会跳过 Lambda 类，因为 JDK 不支持对 Lambda 类进行重转换
     *
     * @param inst Instrumentation 实例
     * @param transformer 类文件转换器，用于修改类的字节码
     * @param classes 需要重转换的类集合
     */
    public static void retransformClasses(Instrumentation inst, ClassFileTransformer transformer,
            Set<Class<?>> classes) {
        try {
            // 添加转换器，设置为可重转换
            inst.addTransformer(transformer, true);

            // 遍历所有需要重转换的类
            for (Class<?> clazz : classes) {
                // 检查是否为 Lambda 类
                if (ClassUtils.isLambdaClass(clazz)) {
                    logger.info(
                            "ignore lambda class: {}, because jdk do not support retransform lambda class: https://github.com/alibaba/arthas/issues/1512.",
                            clazz.getName());
                    continue;
                }
                try {
                    // 对类进行重转换
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    // 记录重转换失败的错误
                    String errorMsg = "retransformClasses class error, name: " + clazz.getName();
                    logger.error(errorMsg, e);
                }
            }
        } finally {
            // 无论成功与否，都要移除转换器
            inst.removeTransformer(transformer);
        }
    }

    /**
     * 触发类重转换
     * 根据类名集合从所有已加载的类中查找并触发重转换
     * 此方法不使用自定义转换器，而是触发类上已注册的转换器
     *
     * @param inst Instrumentation 实例
     * @param classes 需要重转换的类名集合
     */
    public static void trigerRetransformClasses(Instrumentation inst, Collection<String> classes) {
        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 检查类名是否在要重转换的集合中
            if (classes.contains(clazz.getName())) {
                try {
                    // 触发类的重转换
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    // 记录重转换失败的错误
                    String errorMsg = "retransformClasses class error, name: " + clazz.getName();
                    logger.error(errorMsg, e);
                }
            }
        }
    }
}
