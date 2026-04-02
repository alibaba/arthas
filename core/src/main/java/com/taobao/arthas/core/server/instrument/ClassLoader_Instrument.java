package com.taobao.arthas.core.server.instrument;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

/**
 * ClassLoader类的字节码增强类
 * 使用ByteKit库对java.lang.ClassLoader类进行增强，
 * 拦截loadClass方法以实现自定义的类加载逻辑
 *
 * 该增强主要用于处理"java.arthas."开头的类，使其能够通过扩展类加载器加载，
 * 避免类加载冲突并确保Arthas的类能够正确加载
 *
 * @see java.lang.ClassLoader#loadClass(String)
 * @author hengyunabc 2020-11-30
 *
 */
@Instrument(Class = "java.lang.ClassLoader")
public abstract class ClassLoader_Instrument {
    /**
     * 增强后的loadClass方法
     * 在原始的ClassLoader.loadClass方法执行前，拦截类加载请求
     *
     * 工作流程：
     * 1. 如果类名以"java.arthas."开头，尝试使用扩展类加载器加载
     * 2. 如果扩展类加载器存在，委托它加载该类
     * 3. 否则，调用原始的loadClass方法继续执行
     *
     * @param name 要加载的类的全限定名
     * @return 加载的Class对象
     * @throws ClassNotFoundException 如果类无法被找到
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // 检查是否是Arthas相关的类（以java.arthas.开头）
        if (name.startsWith("java.arthas.")) {
            // 获取系统类加载器的父类加载器，即扩展类加载器（Extension ClassLoader）
            ClassLoader extClassLoader = ClassLoader.getSystemClassLoader().getParent();
            // 如果扩展类加载器存在，委托它来加载这个类
            if (extClassLoader != null) {
                return extClassLoader.loadClass(name);
            }
        }

        // 对于非Arthas相关的类，或者扩展类加载器不存在的情况
        // 调用原始的loadClass方法，继续正常的类加载流程
        Class clazz = InstrumentApi.invokeOrigin();
        return clazz;
    }
}
