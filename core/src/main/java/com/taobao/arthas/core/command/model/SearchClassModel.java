package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.List;

/**
 * 搜索类命令的模型
 * 用于封装搜索类的结果信息，包括类的详细信息、类加载器信息等
 *
 * Class info of SearchClassCommand
 * @author gongdewei 2020/04/08
 */
public class SearchClassModel extends ResultModel {

    /**
     * 类详细信息视图对象
     * 包含类的完整信息，如类名、类加载器、注解、字段、方法等
     */
    private ClassDetailVO classInfo;

    /**
     * 是否包含字段信息
     * 为 true 时会在结果中包含类的字段信息
     */
    private boolean withField;

    /**
     * 是否显示详细信息
     * 为 true 时会显示类的完整详细信息
     */
    private boolean detailed;

    /**
     * 匹配到的类名列表
     * 当搜索条件匹配多个类时，返回所有匹配的类名
     */
    private List<String> classNames;

    /**
     * 分段标识
     * 用于分批显示大量搜索结果时的分段标记
     */
    private int segment;

    /**
     * 匹配到的类加载器集合
     * 包含加载了目标类的所有类加载器信息
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 表示用于加载目标类的类加载器的类名
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * 创建一个空的搜索类模型
     */
    public SearchClassModel() {
    }

    /**
     * 构造函数 - 用于包含类详细信息的情况
     *
     * @param classInfo 类详细信息视图对象
     * @param detailed 是否显示详细信息
     * @param withField 是否包含字段信息
     */
    public SearchClassModel(ClassDetailVO classInfo, boolean detailed, boolean withField) {
        this.classInfo = classInfo;
        this.detailed = detailed;
        this.withField = withField;
    }

    /**
     * 构造函数 - 用于包含类名列表的情况
     *
     * @param classNames 匹配到的类名列表
     * @param segment 分段标识
     */
    public SearchClassModel(List<String> classNames, int segment) {
        this.classNames = classNames;
        this.segment = segment;
    }

    /**
     * 获取结果模型的类型标识
     * 用于前端识别结果类型并进行相应的展示处理
     *
     * @return 类型标识字符串，固定返回 "sc"（Search Class 的缩写）
     */
    @Override
    public String getType() {
        return "sc";
    }

    /**
     * 获取类详细信息
     *
     * @return 类详细信息视图对象
     */
    public ClassDetailVO getClassInfo() {
        return classInfo;
    }

    /**
     * 设置类详细信息
     *
     * @param classInfo 类详细信息视图对象
     */
    public void setClassInfo(ClassDetailVO classInfo) {
        this.classInfo = classInfo;
    }

    /**
     * 获取匹配到的类名列表
     *
     * @return 类名列表
     */
    public List<String> getClassNames() {
        return classNames;
    }

    /**
     * 设置匹配到的类名列表
     *
     * @param classNames 类名列表
     */
    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
    }

    /**
     * 获取分段标识
     *
     * @return 分段标识
     */
    public int getSegment() {
        return segment;
    }

    /**
     * 设置分段标识
     *
     * @param segment 分段标识
     */
    public void setSegment(int segment) {
        this.segment = segment;
    }

    /**
     * 判断是否显示详细信息
     *
     * @return true 表示显示详细信息，false 表示不显示详细信息
     */
    public boolean isDetailed() {
        return detailed;
    }

    /**
     * 判断是否包含字段信息
     *
     * @return true 表示包含字段信息，false 表示不包含字段信息
     */
    public boolean isWithField() {
        return withField;
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
    public SearchClassModel setClassLoaderClass(String classLoaderClass) {
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
    public SearchClassModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
