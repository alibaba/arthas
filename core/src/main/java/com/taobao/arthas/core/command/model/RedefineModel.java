package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Redefine命令的模型类
 * <p>
 * 用于封装redefine命令的执行结果。该命令允许在不重启JVM的情况下
 * 重新定义已经加载的类，实现热更新功能。
 * </p>
 * <p>
 * Redefine功能基于JVM的HotSwap机制，可以：
 * <ul>
 *   <li>修改类的方法实现（在方法内部添加/修改代码）</li>
 *   <li>不适用于修改类结构（如添加/删除字段、方法）</li>
 *   <li>通常用于紧急bug修复，避免重启服务</li>
 * </ul>
 * </p>
 * <p>
 * 注意事项：
 * <ul>
 *   <li>只能修改方法体，不能修改类结构</li>
 *   <li>不能添加/删除字段或方法</li>
 *   <li>不能修改继承关系</li>
 *   <li>某些JVM实现可能有额外限制</li>
 * </ul>
 * </p>
 *
 * @author gongdewei 2020/4/16
 */
public class RedefineModel extends ResultModel {

    /**
     * 类重新定义的次数
     * <p>
     * 记录成功重新定义的类数量，用于统计和验证操作结果。
     * 每成功重新定义一个类，此计数器加1。
     * </p>
     */
    private int redefinitionCount;

    /**
     * 已重新定义的类名列表
     * <p>
     * 存储所有成功重新定义的类的完全限定名（Fully Qualified Name）。
     * 例如：["com.example.MyClass", "com.example.service.UserService"]
     * </p>
     */
    private List<String> redefinedClasses;

    /**
     * 匹配的类加载器集合
     * <p>
     * 存储匹配到的类加载器信息，用于显示哪些类加载器加载了目标类。
     * 在Java应用中，同一个类可能被多个类加载器加载，这个字段帮助
     * 用户识别实际操作的类加载器。
     * </p>
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * <p>
     * 指定用于重新定义操作的类加载器的类名。
     * 例如："sun.misc.Launcher$AppClassLoader"
     * </p>
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的RedefineModel实例，初始化类名列表为空的ArrayList。
     * </p>
     */
    public RedefineModel() {
        // 初始化已重新定义的类名列表为空列表
        redefinedClasses = new ArrayList<String>();
    }

    /**
     * 添加一个已重新定义的类
     * <p>
     * 将类名添加到已重新定义的类列表中，并增加重新定义计数。
     * </p>
     *
     * @param className 已重新定义的类的完全限定名
     */
    public void addRedefineClass(String className) {
        // 将类名添加到列表中
        redefinedClasses.add(className);
        // 增加重新定义计数
        redefinitionCount++;
    }

    /**
     * 获取类重新定义的次数
     *
     * @return 已成功重新定义的类数量
     */
    public int getRedefinitionCount() {
        return redefinitionCount;
    }

    /**
     * 设置类重新定义的次数
     * <p>
     * 直接设置重新定义计数，通常用于批量操作后设置最终结果。
     * </p>
     *
     * @param redefinitionCount 要设置的重新定义次数
     */
    public void setRedefinitionCount(int redefinitionCount) {
        this.redefinitionCount = redefinitionCount;
    }

    /**
     * 获取已重新定义的类名列表
     *
     * @return 已重新定义的类名列表，不会为null
     */
    public List<String> getRedefinedClasses() {
        return redefinedClasses;
    }

    /**
     * 设置已重新定义的类名列表
     * <p>
     * 直接设置整个类名列表，会覆盖之前的列表内容。
     * </p>
     *
     * @param redefinedClasses 要设置的类名列表
     */
    public void setRedefinedClasses(List<String> redefinedClasses) {
        this.redefinedClasses = redefinedClasses;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的类名，可能为null
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名（支持链式调用）
     * <p>
     * 设置类加载器类名并返回当前对象，支持链式调用。
     * 例如：model.setClassLoaderClass("MyClassLoader").setMatchedClassLoaders(loaders);
     * </p>
     *
     * @param classLoaderClass 类加载器的类名
     * @return 当前RedefineModel实例
     */
    public RedefineModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        // 返回当前对象以支持链式调用
        return this;
    }

    /**
     * 获取匹配的类加载器集合
     *
     * @return 匹配的类加载器集合，可能为null
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配的类加载器集合（支持链式调用）
     * <p>
     * 设置类加载器集合并返回当前对象，支持链式调用。
     * </p>
     *
     * @param matchedClassLoaders 要设置的类加载器集合
     * @return 当前RedefineModel实例
     */
    public RedefineModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        // 返回当前对象以支持链式调用
        return this;
    }

    /**
     * 获取模型类型
     * <p>
     * 返回此模型的类型标识符，用于前端识别如何渲染此模型数据。
     * </p>
     *
     * @return 模型类型字符串，固定返回"redefine"
     */
    @Override
    public String getType() {
        return "redefine";
    }

}
