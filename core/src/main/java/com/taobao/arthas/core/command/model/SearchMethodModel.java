package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * 搜索方法命令的模型
 * 用于封装搜索方法的结果信息，包括方法的详细信息、类加载器信息等
 *
 * Model of SearchMethodCommand
 * @author gongdewei 2020/4/9
 */
public class SearchMethodModel extends ResultModel {

    /**
     * 方法信息视图对象
     * 包含方法的完整信息，如方法名、参数类型、返回类型、注解等
     */
    private MethodVO methodInfo;

    /**
     * 是否显示详细信息
     * 为 true 时会显示方法的完整详细信息
     */
    private boolean detail;

    /**
     * 匹配到的类加载器集合
     * 包含加载了包含目标方法的类的所有类加载器信息
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 表示用于加载包含目标方法的类的类加载器的类名
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * 创建一个空的搜索方法模型
     */
    public SearchMethodModel() {
    }

    /**
     * 构造函数
     *
     * @param methodInfo 方法信息视图对象
     * @param detail 是否显示详细信息
     */
    public SearchMethodModel(MethodVO methodInfo, boolean detail) {
        this.methodInfo = methodInfo;
        this.detail = detail;
    }

    /**
     * 获取方法信息
     *
     * @return 方法信息视图对象
     */
    public MethodVO getMethodInfo() {
        return methodInfo;
    }

    /**
     * 设置方法信息
     *
     * @param methodInfo 方法信息视图对象
     */
    public void setMethodInfo(MethodVO methodInfo) {
        this.methodInfo = methodInfo;
    }

    /**
     * 判断是否显示详细信息
     *
     * @return true 表示显示详细信息，false 表示不显示详细信息
     */
    public boolean isDetail() {
        return detail;
    }

    /**
     * 设置是否显示详细信息
     *
     * @param detail true 表示显示详细信息，false 表示不显示详细信息
     */
    public void setDetail(boolean detail) {
        this.detail = detail;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的类名
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     *
     * @param classLoaderClass 类加载器的类名
     * @return 当前对象，支持链式调用
     */
    public SearchMethodModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    /**
     * 获取匹配到的类加载器集合
     *
     * @return 匹配到的类加载器集合
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配到的类加载器集合
     *
     * @param matchedClassLoaders 匹配到的类加载器集合
     * @return 当前对象，支持链式调用
     */
    public SearchMethodModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    /**
     * 获取结果模型的类型标识
     * 用于前端识别结果类型并进行相应的展示处理
     *
     * @return 类型标识字符串，固定返回 "sm"（Search Method 的缩写）
     */
    @Override
    public String getType() {
        return "sm";
    }
}
