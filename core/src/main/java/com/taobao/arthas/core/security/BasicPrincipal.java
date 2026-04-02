package com.taobao.arthas.core.security;

import java.security.Principal;

/**
 * 基础认证（Basic Auth）的身份主体类
 * 实现了Java标准的Principal接口，用于存储用户名和密码凭据
 * 用于HTTP Basic认证场景
 *
 * @author hengyunabc 2021-03-04
 */
public final class BasicPrincipal implements Principal {

    /**
     * 用户名，标识认证主体的名称
     */
    private final String username;

    /**
     * 密码，用于验证身份的凭据
     */
    private final String password;

    /**
     * 构造函数，创建BasicPrincipal实例
     *
     * @param username 用户名，不能为null
     * @param password 密码，不能为null
     */
    public BasicPrincipal(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * 获取主体的名称
     * 这是Principal接口的方法实现，返回用户名作为主体名称
     *
     * @return 用户名
     */
    @Override
    public String getName() {
        return username;
    }

    /**
     * 获取用户名
     *
     * @return 用户名字符串
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取密码
     *
     * @return 密码字符串
     */
    public String getPassword() {
        return password;
    }

    /**
     * 计算哈希码
     * 基于用户名和密码生成哈希码，用于在哈希集合中存储和比较
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        final int prime = 31; // 使用质数31作为哈希计算的乘数
        int result = 1;
        // 将密码的哈希码累加到结果中（如果密码为null则使用0）
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        // 将用户名的哈希码累加到结果中（如果用户名为null则使用0）
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    /**
     * 判断两个对象是否相等
     * 两个BasicPrincipal对象相等的条件是：用户名和密码都相同
     *
     * @param obj 要比较的对象
     * @return 如果对象相等返回true，否则返回false
     */
    @Override
    public boolean equals(Object obj) {
        // 如果是同一个对象引用，直接返回true
        if (this == obj)
            return true;
        // 如果要比较的对象为null，返回false
        if (obj == null)
            return false;
        // 如果类型不同，返回false
        if (getClass() != obj.getClass())
            return false;
        // 类型转换为BasicPrincipal进行比较
        BasicPrincipal other = (BasicPrincipal) obj;
        // 比较密码字段
        if (password == null) {
            // 当前密码为null，对方密码不为null则不相等
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            // 当前密码不为null，与对方密码比较
            return false;
        // 比较用户名字段
        if (username == null) {
            // 当前用户名为null，对方用户名不为null则不相等
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            // 当前用户名不为null，与对方用户名比较
            return false;
        // 用户名和密码都相等
        return true;
    }

    /**
     * 转换为字符串表示
     * 为了安全起见，字符串中不显示密码，只显示用户名
     *
     * @return 格式为"BasicPrincipal[username]"的字符串
     */
    @Override
    public String toString() {
        // 出于安全考虑，不在日志中显示密码
        return "BasicPrincipal[" + username + "]";
    }
}