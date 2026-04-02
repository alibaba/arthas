package com.taobao.arthas.core.util.affect;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.ClassLoaderUtils;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * 类增强影响范围统计类<br/>
 * 用于统计和记录代码增强操作的影响范围，包括增强的类数量、方法数量、耗时等信息
 * 该类继承自Affect基类，专门用于跟踪Arthas增强操作的影响统计
 * Created by vlinux on 15/5/19.
 * @author hengyunabc 2020-06-01
 */
public final class EnhancerAffect extends Affect {

    /**
     * 增强的类数量计数器
     * 使用 AtomicInteger 保证在多线程环境下的原子性操作
     */
    private final AtomicInteger cCnt = new AtomicInteger();

    /**
     * 增强的方法数量计数器
     * 使用 AtomicInteger 保证在多线程环境下的原子性操作
     */
    private final AtomicInteger mCnt = new AtomicInteger();

    /**
     * 类文件转换器
     * 用于在类加载时修改类的字节码，实现代码增强功能
     */
    private ClassFileTransformer transformer;

    /**
     * 监听器ID
     * 用于唯一标识此次增强操作对应的监听器
     */
    private long listenerId;

    /**
     * 增强过程中发生的异常
     * 如果增强过程中出现错误，会将异常对象保存在此字段中
     */
    private Throwable throwable;

    /**
     * dumpClass的文件存放集合
     * 当开启dump选项时，将增强后的类文件转储到磁盘，此集合保存所有dump文件
     */
    private final Collection<File> classDumpFiles = new ArrayList<File>();

    /**
     * 影响的方法列表
     * 记录所有被增强的方法的详细信息，格式为：
     * 类加载器Hash|类名#方法名|方法描述符
     */
    private final List<String> methods = new ArrayList<String>();

    /**
     * 超出限制的消息
     * 当增强的类或方法数量超过限制时，保存提示信息
     */
    private String overLimitMsg;

    /**
     * 默认构造函数
     * 初始化一个空的增强影响统计对象
     */
    public EnhancerAffect() {
    }

    /**
     * 增加并获取增强类数量
     * 原子性地增加类计数器并返回更新后的值
     *
     * @param cc 类影响计数的增量，可以为负数
     * @return 增加后的当前影响类总数
     */
    public int cCnt(int cc) {
        return cCnt.addAndGet(cc);
    }

    /**
     * 增加并获取增强方法数量
     * 原子性地增加方法计数器并返回更新后的值
     *
     * @param mc 方法影响计数的增量，可以为负数
     * @return 增加后的当前影响方法总数
     */
    public int mCnt(int mc) {
        return mCnt.addAndGet(mc);
    }

    /**
     * 记录被影响的方法详细信息，并自动增加方法计数
     * 该方法将方法的完整信息添加到methods列表中，格式为：
     * "ClassLoaderHash|类名#方法名|方法描述符"
     * 例如："1234|com.example.Test#doSomething|(Ljava/lang/String;)V"
     *
     * @param classLoader 方法所属的类加载器
     * @param clazz 类的内部名称（可能使用/分隔符）
     * @param method 方法名
     * @param methodDesc 方法描述符（JVM格式的参数和返回类型描述）
     * @return 增加后的当前影响方法总数
     */
    public int addMethodAndCount(ClassLoader classLoader, String clazz, String method, String methodDesc) {
        // 构造方法标识字符串：类加载器Hash | 类名（将/替换为.）# 方法名 | 方法描述符
        this.methods.add(ClassLoaderUtils.classLoaderHash(classLoader) + "|" + clazz.replace('/', '.') + "#" + method + "|" + methodDesc);
        // 方法计数器加1
        return mCnt.addAndGet(1);
    }

    /**
     * 获取当前增强类的总数
     *
     * @return 增强类的个数
     */
    public int cCnt() {
        return cCnt.get();
    }

    /**
     * 获取当前增强方法的总数
     *
     * @return 增强方法的个数
     */
    public int mCnt() {
        return mCnt.get();
    }

    /**
     * 添加dump的类文件
     * 当开启dump选项时，将增强后的类文件路径添加到集合中
     *
     * @param file dump的类文件对象
     */
    public void addClassDumpFile(File file) {
        classDumpFiles.add(file);
    }

    /**
     * 获取类文件转换器
     * 转换器负责在类加载时修改字节码
     *
     * @return ClassFileTransformer实例
     */
    public ClassFileTransformer getTransformer() {
        return transformer;
    }

    /**
     * 设置类文件转换器
     *
     * @param transformer 要设置的ClassFileTransformer实例
     */
    public void setTransformer(ClassFileTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * 获取监听器ID
     *
     * @return 监听器ID
     */
    public long getListenerId() {
        return listenerId;
    }

    /**
     * 设置监听器ID
     *
     * @param listenerId 监听器ID
     */
    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    /**
     * 获取增强过程中抛出的异常
     *
     * @return Throwable对象，如果没有异常则为null
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 设置增强过程中抛出的异常
     *
     * @param throwable 异常对象
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * 获取所有dump的类文件集合
     *
     * @return 类文件集合
     */
    public Collection<File> getClassDumpFiles() {
        return classDumpFiles;
    }

    /**
     * 获取所有影响的方法列表
     *
     * @return 方法信息列表
     */
    public List<String> getMethods() {
        return methods;
    }

    /**
     * 获取超出限制的提示消息
     *
     * @return 超限消息文本
     */
    public String getOverLimitMsg() {
        return overLimitMsg;
    }

    /**
     * 设置超出限制的提示消息
     *
     * @param overLimitMsg 超限消息文本
     */
    public void setOverLimitMsg(String overLimitMsg) {
        this.overLimitMsg = overLimitMsg;
    }

    /**
     * 生成增强影响统计的字符串表示
     * 包含dump文件列表、影响的方法列表、统计摘要和错误信息（如果有）
     * 输出格式取决于全局配置选项
     *
     * @return 格式化的统计信息字符串
     */
    @Override
    public String toString() {
        //TODO removing EnhancerAffect.toString(), replace with ViewRenderUtil.renderEnhancerAffect()
        // 创建字符串构建器用于拼接输出信息
        final StringBuilder infoSB = new StringBuilder();

        // 如果开启了dump选项且存在dump文件，列出所有dump文件
        if (GlobalOptions.isDump
                && !classDumpFiles.isEmpty()) {

            // 遍历所有dump的类文件
            for (File classDumpFile : classDumpFiles) {
                // 添加dump文件的绝对路径
                infoSB.append("[dump: ").append(classDumpFile.getAbsoluteFile()).append("]\n");
            }
        }

        // 如果开启了详细输出模式且存在影响的方法，列出所有方法
        if (GlobalOptions.verbose && !methods.isEmpty()) {
            // 遍历所有被影响的方法
            for (String method : methods) {
                // 添加方法的完整信息
                infoSB.append("[Affect method: ").append(method).append("]\n");
            }
        }

        // 添加统计摘要：类数量、方法数量、耗时（毫秒）、监听器ID
        infoSB.append(format("Affect(class count: %d , method count: %d) cost in %s ms, listenerId: %d",
                cCnt(),              // 增强的类总数
                mCnt(),              // 增强的方法总数
                cost(),              // 从父类Affect继承，获取操作耗时
                listenerId));        // 监听器ID

        // 如果在增强过程中发生了异常，添加异常信息
        if (this.throwable != null) {
            infoSB.append("\nEnhance error! exception: ").append(this.throwable);
        }

        // 返回完整的统计信息字符串
        return infoSB.toString();
    }

}
