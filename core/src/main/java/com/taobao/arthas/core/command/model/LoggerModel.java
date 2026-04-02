package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.Map;

/**
 * Logger命令的结果模型类
 *
 * 该类用于封装logger命令的执行结果，包括日志信息、类加载器信息等
 * 继承自ResultModel基类，用于命令执行结果的统一管理和传输
 *
 * @author gongdewei 2020/4/22
 */
public class LoggerModel extends ResultModel {

    /**
     * 日志信息映射表
     * 键为日志名称，值为包含日志详细信息的Map（如级别、处理器等信息）
     */
    private Map<String, Map<String, Object>> loggerInfoMap;

    /**
     * 匹配的类加载器集合
     * 存储所有匹配到的类加载器信息
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 记录当前使用的类加载器的完整类名
     */
    private String classLoaderClass;

    /**
     * 默认构造函数
     * 创建一个空的LoggerModel实例
     */
    public LoggerModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param loggerInfoMap 日志信息映射表
     */
    public LoggerModel(Map<String, Map<String, Object>> loggerInfoMap) {
        this.loggerInfoMap = loggerInfoMap;
    }

    /**
     * 获取日志信息映射表
     *
     * @return 日志信息映射表，键为日志名称，值为日志详细信息
     */
    public Map<String, Map<String, Object>> getLoggerInfoMap() {
        return loggerInfoMap;
    }

    /**
     * 设置日志信息映射表
     *
     * @param loggerInfoMap 要设置的日志信息映射表
     */
    public void setLoggerInfoMap(Map<String, Map<String, Object>> loggerInfoMap) {
        this.loggerInfoMap = loggerInfoMap;
    }

    /**
     * 获取类加载器的类名
     *
     * @return 类加载器的完整类名
     */
    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    /**
     * 设置类加载器的类名
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param classLoaderClass 类加载器的完整类名
     * @return 当前LoggerModel实例，支持链式调用
     */
    public LoggerModel setClassLoaderClass(String classLoaderClass) {
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
     * 使用链式调用风格，方便连续设置多个属性
     *
     * @param matchedClassLoaders 匹配的类加载器集合
     * @return 当前LoggerModel实例，支持链式调用
     */
    public LoggerModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    /**
     * 获取模型类型标识
     * 用于在序列化和反序列化时识别模型类型
     *
     * @return 类型标识字符串，固定返回"logger"
     */
    @Override
    public String getType() {
        return "logger";
    }

}
