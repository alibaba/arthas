package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.List;

/**
 * Dump类命令模型
 * <p>
 * 该类用于封装dump命令的执行结果，dump命令用于将JVM中已加载的类的字节码导出到文件系统中。
 * 该模型包含了已成功dump的类信息、匹配到的类信息、匹配到的类加载器信息以及类加载器的类名等信息。
 * 该模型继承自ResultModel，可以与Arthas的结果展示框架无缝集成，支持链式调用以便于构建对象。
 * </p>
 *
 * @author gongdewei 2020/4/21
 */
public class DumpClassModel extends ResultModel {

    /**
     * 已dump的类列表
     * <p>
     * 存储已成功导出到文件系统的类的详细信息，包括类名、类加载器、dump文件路径等。
     * 每个DumpClassVO对象代表一个成功dump的类的完整信息。
     * </p>
     */
    private List<DumpClassVO> dumpedClasses;

    /**
     * 匹配到的类集合
     * <p>
     * 存储根据用户提供的类名模式匹配到的所有类的信息，包括类名、类加载器、超级类等。
     * 这些类可能已经被dump，也可能因为某些原因（如类定义来自启动类加载器）无法dump。
     * </p>
     */
    private Collection<ClassVO> matchedClasses;

    /**
     * 匹配到的类加载器集合
     * <p>
     * 存储根据用户提供的类加载器哈希值匹配到的所有类加载器的信息。
     * 用于展示匹配到的类加载器的详细情况，帮助用户确认dump操作的目标范围。
     * </p>
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * <p>
     * 存储用于匹配类加载器的类全限定名。当用户通过类加载器的类名来限定dump范围时，
     * 会使用此属性记录用户指定的类加载器类名，以便在结果中明确dump操作的作用域。
     * </p>
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的DumpClassModel实例，所有属性初始化为null。
     * 可以通过链式调用的setter方法逐步设置各个属性值。
     * </p>
     */
    public DumpClassModel() {
    }

    /**
     * 获取模型类型
     * <p>
     * 返回此结果模型的类型标识符，用于在前端或客户端识别和区分不同类型的命令结果。
     * dump命令的固定返回值为"dump"。
     * </p>
     *
     * @return 模型类型标识符，固定返回"dump"
     */
    @Override
    public String getType() {
        // 返回dump命令的类型标识符
        return "dump";
    }

    /**
     * 获取已dump的类列表
     * <p>
     * 返回已成功导出到文件系统的类的详细信息列表，可以用于展示dump操作的结果，
     * 或者在需要时读取dump的类文件进行进一步分析。
     * </p>
     *
     * @return 已dump的类列表，包含每个成功dump的类的完整信息
     */
    public List<DumpClassVO> getDumpedClasses() {
        return dumpedClasses;
    }

    /**
     * 设置已dump的类列表
     * <p>
     * 设置已成功导出到文件系统的类的详细信息列表。在执行dump命令后，
     * 会将所有成功dump的类的信息收集到列表中并设置到此属性。
     * </p>
     *
     * @param dumpedClasses 已dump的类列表，包含每个成功dump的类的完整信息
     * @return 当前DumpClassModel实例，支持链式调用
     */
    public DumpClassModel setDumpedClasses(List<DumpClassVO> dumpedClasses) {
        this.dumpedClasses = dumpedClasses;
        // 返回当前实例以支持链式调用
        return this;
    }

    /**
     * 获取匹配到的类集合
     * <p>
     * 返回根据用户提供的类名模式匹配到的所有类的信息。
     * 可以用于展示dump操作的搜索范围，帮助用户了解有多少类匹配了指定的模式。
     * </p>
     *
     * @return 匹配到的类集合，包含所有匹配类名的类的详细信息
     */
    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    /**
     * 设置匹配到的类集合
     * <p>
     * 设置根据用户提供的类名模式匹配到的所有类的信息。
     * 在执行dump命令时，会先搜索匹配的类，然后将匹配结果设置到此属性。
     * </p>
     *
     * @param matchedClasses 匹配到的类集合，包含所有匹配类名的类的详细信息
     * @return 当前DumpClassModel实例，支持链式调用
     */
    public DumpClassModel setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
        // 返回当前实例以支持链式调用
        return this;
    }

    /**
     * 获取类加载器的类名
     * <p>
     * 返回用于匹配类加载器的类全限定名。当用户通过-c或--classloader参数指定类加载器时，
     * 会记录类加载器的类名，以便在结果中明确dump操作的作用域。
     * </p>
     *
     * @return 类加载器的类全限定名，如果未指定则返回null
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * <p>
     * 设置用于匹配类加载器的类全限定名。当用户通过-c或--classloader参数指定类加载器时，
     * 会将类加载器的类名设置到此属性，用于限定dump操作的范围。
     * </p>
     *
     * @param classLoaderClass 类加载器的类全限定名
     * @return 当前DumpClassModel实例，支持链式调用
     */
    public DumpClassModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        // 返回当前实例以支持链式调用
        return this;
    }

    /**
     * 获取匹配到的类加载器集合
     * <p>
     * 返回根据用户提供的类加载器哈希值匹配到的所有类加载器的信息。
     * 可以用于展示匹配到的类加载器的详细情况，帮助用户确认dump操作的目标范围。
     * </p>
     *
     * @return 匹配到的类加载器集合，包含所有匹配哈希值的类加载器的详细信息
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配到的类加载器集合
     * <p>
     * 设置根据用户提供的类加载器哈希值匹配到的所有类加载器的信息。
     * 在执行dump命令时，如果用户指定了类加载器，会先搜索匹配的类加载器，
     * 然后将匹配结果设置到此属性。
     * </p>
     *
     * @param matchedClassLoaders 匹配到的类加载器集合，包含所有匹配哈希值的类加载器的详细信息
     * @return 当前DumpClassModel实例，支持链式调用
     */
    public DumpClassModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        // 返回当前实例以支持链式调用
        return this;
    }

}
