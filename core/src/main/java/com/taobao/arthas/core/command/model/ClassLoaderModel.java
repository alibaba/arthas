package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.ClassLoaderStat;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.ClassLoaderUrlStat;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.UrlClassStat;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 类加载器模型
 *
 * 继承自ResultModel，用于封装类加载器相关的命令执行结果。
 * 支持多种类加载器查询场景，包括类集查询、资源查询、URL查询、类加载器树形结构展示等。
 * 该模型通过不同的属性组合来支持多种类加载器相关命令的输出。
 *
 * @author gongdewei 2020/4/21
 */
public class ClassLoaderModel extends ResultModel {

    /**
     * 类集合信息
     * 用于存储查询到的类的集合信息
     */
    private ClassSetVO classSet;

    /**
     * 资源列表
     * 用于存储查询到的资源路径列表
     */
    private List<String> resources;

    /**
     * 加载的类详情
     * 用于存储某个被加载的类的详细信息
     */
    private ClassDetailVO loadClass;

    /**
     * URL列表
     * 用于存储类加载器的URL路径列表
     */
    private List<String> urls;

    /**
     * 类加载器列表
     * 用于存储查询到的类加载器信息（对应classloader -l -t命令）
     */
    private List<ClassLoaderVO> classLoaders;

    /**
     * 是否以树形结构展示
     * 标识是否需要以树形结构展示类加载器层级关系
     */
    private Boolean tree;

    /**
     * 类加载器统计信息
     * 存储类加载器的统计信息，key为类加载器标识，value为统计数据
     */
    private Map<String, ClassLoaderStat> classLoaderStats;

    /**
     * 匹配的类加载器集合
     * 存储符合查询条件的类加载器集合
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 用于按类名匹配类加载器
     */
    private String classLoaderClass;

    /**
     * URL统计信息
     * 存储类加载器URL的统计信息（对应classloader -u命令）
     */
    private Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats;

    /**
     * 类加载器对象
     * 存储特定的类加载器对象信息（用于url->classes统计场景）
     */
    private ClassLoaderVO classLoader;

    /**
     * URL类统计信息列表
     * 存储URL对应的类加载统计信息
     */
    private List<UrlClassStat> urlClassStats;

    /**
     * 是否展示URL类统计详情
     * 标识是否需要展示URL类统计的详细信息
     */
    private Boolean urlClassStatsDetail;

    /**
     * 默认构造函数
     * 创建一个空的类加载器模型对象
     */
    public ClassLoaderModel() {
    }

    /**
     * 获取模型类型
     * 用于标识这是类加载器类型的模型
     *
     * @return 模型类型标识字符串 "classloader"
     */
    @Override
    public String getType() {
        return "classloader";
    }

    /**
     * 获取类集合信息
     *
     * @return 类集合信息对象
     */
    public ClassSetVO getClassSet() {
        return classSet;
    }

    /**
     * 设置类集合信息
     * 支持链式调用
     *
     * @param classSet 类集合信息对象
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setClassSet(ClassSetVO classSet) {
        this.classSet = classSet;
        return this;
    }

    /**
     * 获取资源列表
     *
     * @return 资源路径列表
     */
    public List<String> getResources() {
        return resources;
    }

    /**
     * 设置资源列表
     * 支持链式调用
     *
     * @param resources 资源路径列表
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setResources(List<String> resources) {
        this.resources = resources;
        return this;
    }

    /**
     * 获取加载的类详情
     *
     * @return 类详情对象
     */
    public ClassDetailVO getLoadClass() {
        return loadClass;
    }

    /**
     * 设置加载的类详情
     * 支持链式调用
     *
     * @param loadClass 类详情对象
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setLoadClass(ClassDetailVO loadClass) {
        this.loadClass = loadClass;
        return this;
    }

    /**
     * 获取URL列表
     *
     * @return URL路径列表
     */
    public List<String> getUrls() {
        return urls;
    }

    /**
     * 设置URL列表
     * 支持链式调用
     *
     * @param urls URL路径列表
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setUrls(List<String> urls) {
        this.urls = urls;
        return this;
    }

    /**
     * 获取类加载器列表
     *
     * @return 类加载器信息列表
     */
    public List<ClassLoaderVO> getClassLoaders() {
        return classLoaders;
    }

    /**
     * 设置类加载器列表
     * 支持链式调用
     *
     * @param classLoaders 类加载器信息列表
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setClassLoaders(List<ClassLoaderVO> classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    /**
     * 获取是否以树形结构展示
     *
     * @return 如果以树形结构展示返回true，否则返回false
     */
    public Boolean getTree() {
        return tree;
    }

    /**
     * 设置是否以树形结构展示
     * 支持链式调用
     *
     * @param tree 是否以树形结构展示
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setTree(Boolean tree) {
        this.tree = tree;
        return this;
    }

    /**
     * 获取类加载器统计信息
     *
     * @return 类加载器统计信息映射表，key为类加载器标识
     */
    public Map<String, ClassLoaderStat> getClassLoaderStats() {
        return classLoaderStats;
    }

    /**
     * 设置类加载器统计信息
     * 支持链式调用
     *
     * @param classLoaderStats 类加载器统计信息映射表
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setClassLoaderStats(Map<String, ClassLoaderStat> classLoaderStats) {
        this.classLoaderStats = classLoaderStats;
        return this;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的类名字符串
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * 支持链式调用
     *
     * @param classLoaderClass 类加载器的类名字符串
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    /**
     * 获取匹配的类加载器集合
     *
     * @return 匹配的类加载器集合
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配的类加载器集合
     * 支持链式调用
     *
     * @param matchedClassLoaders 匹配的类加载器集合
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    /**
     * 获取URL统计信息
     *
     * @return URL统计信息映射表，key为类加载器对象
     */
    public Map<ClassLoaderVO, ClassLoaderUrlStat> getUrlStats() {
        return urlStats;
    }

    /**
     * 设置URL统计信息
     *
     * @param urlStats URL统计信息映射表
     */
    public void setUrlStats(Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats) {
        this.urlStats = urlStats;
    }

    /**
     * 获取类加载器对象
     *
     * @return 类加载器对象
     */
    public ClassLoaderVO getClassLoader() {
        return classLoader;
    }

    /**
     * 设置类加载器对象
     * 支持链式调用
     *
     * @param classLoader 类加载器对象
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setClassLoader(ClassLoaderVO classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * 获取URL类统计信息列表
     *
     * @return URL类统计信息列表
     */
    public List<UrlClassStat> getUrlClassStats() {
        return urlClassStats;
    }

    /**
     * 设置URL类统计信息列表
     * 支持链式调用
     *
     * @param urlClassStats URL类统计信息列表
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setUrlClassStats(List<UrlClassStat> urlClassStats) {
        this.urlClassStats = urlClassStats;
        return this;
    }

    /**
     * 获取是否展示URL类统计详情
     *
     * @return 如果展示详情返回true，否则返回false
     */
    public Boolean getUrlClassStatsDetail() {
        return urlClassStatsDetail;
    }

    /**
     * 设置是否展示URL类统计详情
     * 支持链式调用
     *
     * @param urlClassStatsDetail 是否展示URL类统计详情
     * @return 当前ClassLoaderModel对象，支持链式调用
     */
    public ClassLoaderModel setUrlClassStatsDetail(Boolean urlClassStatsDetail) {
        this.urlClassStatsDetail = urlClassStatsDetail;
        return this;
    }

}
