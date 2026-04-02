package com.taobao.arthas.core.security;

import java.security.Principal;

/**
 * Bearer令牌（Bearer Token）的身份主体类
 * 实现了Java标准的Principal接口，用于存储Bearer令牌凭据
 * 用于OAuth 2.0等Bearer Token认证场景
 */
public final class BearerPrincipal implements Principal {

    /**
     * Bearer令牌字符串
     * 用于在API请求中证明调用者的身份和权限
     */
    private final String token;

    /**
     * 构造函数，创建BearerPrincipal实例
     *
     * @param token Bearer令牌字符串
     */
    public BearerPrincipal(String token) {
        this.token = token;
    }

    /**
     * 获取主体的名称
     * 这是Principal接口的方法实现，返回固定的"bearer"字符串
     * 表示这是Bearer类型的认证主体
     *
     * @return 固定返回"bearer"字符串
     */
    @Override
    public String getName() {
        return "bearer";
    }

    /**
     * 获取Bearer令牌
     *
     * @return 令牌字符串
     */
    public String getToken() {
        return token;
    }

    /**
     * 计算哈希码
     * 基于令牌字符串生成哈希码，用于在哈希集合中存储和比较
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        final int prime = 31; // 使用质数31作为哈希计算的乘数
        int result = 1;
        // 将令牌的哈希码累加到结果中（如果令牌为null则使用0）
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    /**
     * 判断两个对象是否相等
     * 两个BearerPrincipal对象相等的条件是：令牌字符串相同
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
        // 类型转换为BearerPrincipal进行比较
        BearerPrincipal other = (BearerPrincipal) obj;
        // 比较令牌字段
        if (token == null) {
            // 当前令牌为null，对方令牌不为null则不相等
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            // 当前令牌不为null，与对方令牌比较
            return false;
        // 令牌相等
        return true;
    }

    /**
     * 转换为字符串表示
     * 出于安全考虑，字符串中不显示实际的令牌内容，而是用***代替
     * 防止令牌在日志或调试信息中泄露
     *
     * @return 格式为"BearerPrincipal[***]"的字符串，令牌内容被隐藏
     */
    @Override
    public String toString() {
        // 出于安全原因，不显示令牌内容，使用***代替
        return "BearerPrincipal[***]";
    }
}
