package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * OGNL命令的数据模型
 * 用于封装OgnlCommand的执行结果，包含OGNL表达式求值结果和类加载器信息
 *
 * @author gongdewei 2020/4/29
 */
public class OgnlModel extends ResultModel {
    /**
     * OGNL表达式的求值结果
     * 使用ObjectVO封装，支持复杂对象的序列化和展示
     */
    private ObjectVO value;

    /**
     * 匹配的类加载器集合
     * 存储执行OGNL表达式时匹配到的所有类加载器的视图对象
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 表示执行OGNL表达式时使用的类加载器的完整类名
     */
    private String classLoaderClass;


    /**
     * 获取结果模型的类型标识
     * 用于前端或客户端识别返回的数据类型
     *
     * @return 返回"ognl"字符串标识，表示这是OGNL命令的返回结果
     */
    @Override
    public String getType() {
        return "ognl";
    }

    /**
     * 获取OGNL表达式的求值结果
     *
     * @return OGNL表达式的求值结果，封装在ObjectVO中
     */
    public ObjectVO getValue() {
        return value;
    }

    /**
     * 设置OGNL表达式的求值结果
     * 采用链式调用设计，方便连续设置多个属性
     *
     * @param value OGNL表达式的求值结果，封装在ObjectVO中
     * @return 返回当前OgnlModel实例，支持链式调用
     */
    public OgnlModel setValue(ObjectVO value) {
        this.value = value;
        return this;
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
     * 采用链式调用设计，方便连续设置多个属性
     *
     * @param classLoaderClass 类加载器的完整类名
     * @return 返回当前OgnlModel实例，支持链式调用
     */
    public OgnlModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    /**
     * 获取匹配的类加载器集合
     *
     * @return 匹配的类加载器视图对象集合
     */
    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    /**
     * 设置匹配的类加载器集合
     * 采用链式调用设计，方便连续设置多个属性
     *
     * @param matchedClassLoaders 匹配的类加载器视图对象集合
     * @return 返回当前OgnlModel实例，支持链式调用
     */
    public OgnlModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
