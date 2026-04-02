package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.taobao.arthas.core.command.klass100.RetransformCommand.RetransformEntry;

/**
 * 类重转换结果模型
 * <p>
 * 该模型用于封装类重转换（retransform）命令的执行结果。
 * 类重转换是Java Instrumentation API提供的功能，允许在运行时重新转换已加载的类字节码。
 * 该模型记录了重转换的类信息、类加载器信息、重转换条目等详细数据。
 * </p>
 *
 * @author hengyunabc 2021-01-06
 */
public class RetransformModel extends ResultModel {

    /**
     * 重转换的类数量
     * 记录实际执行了重转换操作的类的总数
     */
    private int retransformCount;

    /**
     * 重转换的类名列表
     * 存储所有执行了重转换操作的类的全限定名列表
     */
    private List<String> retransformClasses;

    /**
     * 匹配的类加载器集合
     * 存储匹配到的类加载器的视图对象（VO）集合，用于展示类加载器信息
     */
    private Collection<ClassLoaderVO> matchedClassLoaders;

    /**
     * 类加载器的类名
     * 指定目标类加载器的类名，用于限定在特定类加载器中进行重转换操作
     */
    private String classLoaderClass;

    /**
     * 重转换条目列表
     * 存储重转换操作的详细条目信息，每个条目包含类的详细配置和状态
     */
    private List<RetransformEntry> retransformEntries;

    /**
     * 重转换条目的ID列表
     * 存储重转换条目的唯一标识符列表，用于管理和索引重转换条目
     */
    private List<Integer> ids;

    /**
     * 被删除的重转换条目
     * 记录最近一次被删除的重转换条目信息
     */
    private RetransformEntry deletedRetransformEntry;

    /**
     * 默认构造函数
     * 创建一个空的RetransformModel对象
     */
    public RetransformModel() {
    }

    /**
     * 获取重转换条目的ID列表
     *
     * @return ID列表
     */
    public List<Integer> getIds() {
        return ids;
    }

    /**
     * 设置重转换条目的ID列表
     *
     * @param ids ID列表
     */
    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    /**
     * 添加一个重转换类到列表中
     * <p>
     * 如果类列表未初始化，则先创建列表，然后添加类名并递增计数器
     * </p>
     *
     * @param className 要添加的类的全限定名
     */
    public void addRetransformClass(String className) {
        // 如果列表为空，则创建新的ArrayList
        if (retransformClasses == null) {
            retransformClasses = new ArrayList<String>();
        }
        // 添加类名到列表
        retransformClasses.add(className);
        // 递增重转换计数器
        retransformCount++;
    }

    /**
     * 获取重转换的类数量
     *
     * @return 重转换的类总数
     */
    public int getRetransformCount() {
        return retransformCount;
    }

    /**
     * 设置重转换的类数量
     *
     * @param retransformCount 重转换的类总数
     */
    public void setRetransformCount(int retransformCount) {
        this.retransformCount = retransformCount;
    }

    /**
     * 获取重转换的类名列表
     *
     * @return 重转换的类名列表
     */
    public List<String> getRetransformClasses() {
        return retransformClasses;
    }

    /**
     * 设置重转换的类名列表
     *
     * @param retransformClasses 重转换的类名列表
     */
    public void setRetransformClasses(List<String> retransformClasses) {
        this.retransformClasses = retransformClasses;
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
     * <p>
     * 该方法支持链式调用，方便连续设置多个属性
     * </p>
     *
     * @param classLoaderClass 类加载器的类名
     * @return 当前RetransformModel对象，支持链式调用
     */
    public RetransformModel setClassLoaderClass(String classLoaderClass) {
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
     * <p>
     * 该方法支持链式调用，方便连续设置多个属性
     * </p>
     *
     * @param matchedClassLoaders 匹配的类加载器视图对象集合
     * @return 当前RetransformModel对象，支持链式调用
     */
    public RetransformModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    /**
     * 获取重转换条目列表
     *
     * @return 重转换条目列表
     */
    public List<RetransformEntry> getRetransformEntries() {
        return retransformEntries;
    }

    /**
     * 设置重转换条目列表
     *
     * @param retransformEntries 重转换条目列表
     */
    public void setRetransformEntries(List<RetransformEntry> retransformEntries) {
        this.retransformEntries = retransformEntries;
    }

    /**
     * 获取被删除的重转换条目
     *
     * @return 被删除的重转换条目对象
     */
    public RetransformEntry getDeletedRetransformEntry() {
        return deletedRetransformEntry;
    }

    /**
     * 设置被删除的重转换条目
     *
     * @param deletedRetransformEntry 被删除的重转换条目对象
     */
    public void setDeletedRetransformEntry(RetransformEntry deletedRetransformEntry) {
        this.deletedRetransformEntry = deletedRetransformEntry;
    }

    /**
     * 获取结果类型标识
     * <p>
     * 用于在序列化和反序列化时标识该模型对象的类型
     * </p>
     *
     * @return 返回"retransform"字符串，标识这是类重转换结果模型
     */
    @Override
    public String getType() {
        return "retransform";
    }

}
