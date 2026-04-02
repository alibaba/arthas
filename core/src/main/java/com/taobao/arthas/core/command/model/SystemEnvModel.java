package com.taobao.arthas.core.command.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * 系统环境变量键值对结果模型
 * <p>
 * 该类用于封装获取或修改系统环境变量命令的执行结果
 * 使用TreeMap存储环境变量，保证键值对的有序性（按字母顺序排序）
 * </p>
 *
 * @author gongdewei 2020/4/2
 */
public class SystemEnvModel extends ResultModel {

    /**
     * 环境变量映射表
     * <p>
     * 使用TreeMap存储环境变量键值对，保证按环境变量名的字母顺序排序
     * Key为环境变量名，Value为环境变量值
     * </p>
     */
    private Map<String, String> env = new TreeMap<String, String>();

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的环境变量模型
     * </p>
     */
    public SystemEnvModel() {
    }

    /**
     * 通过Map构造环境变量模型
     * <p>
     * 将给定的Map中的所有键值对添加到环境变量映射表中
     * </p>
     *
     * @param env 包含环境变量键值对的Map对象
     */
    public SystemEnvModel(Map env) {
        this.putAll(env);
    }

    /**
     * 通过单个键值对构造环境变量模型
     * <p>
     * 创建包含单个环境变量的模型
     * </p>
     *
     * @param name  环境变量名
     * @param value 环境变量值
     */
    public SystemEnvModel(String name, String value) {
        this.put(name, value);
    }

    /**
     * 获取环境变量映射表
     *
     * @return 包含所有环境变量键值对的Map对象
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     * 添加或更新单个环境变量
     * <p>
     * 如果指定的键已存在，则覆盖其值；否则添加新的键值对
     * </p>
     *
     * @param key   环境变量名
     * @param value 环境变量值
     * @return 该键之前关联的值，如果没有则返回null
     */
    public String put(String key, String value) {
        return env.put(key, value);
    }

    /**
     * 批量添加环境变量
     * <p>
     * 将给定Map中的所有键值对添加到环境变量映射表中
     * </p>
     *
     * @param m 包含环境变量键值对的Map对象
     */
    public void putAll(Map m) {
        env.putAll(m);
    }

    /**
     * 获取结果类型
     * <p>
     * 返回"sysenv"标识这是一个系统环境变量命令的结果
     * </p>
     *
     * @return 结果类型字符串"sysenv"
     */
    @Override
    public String getType() {
        return "sysenv";
    }
}
