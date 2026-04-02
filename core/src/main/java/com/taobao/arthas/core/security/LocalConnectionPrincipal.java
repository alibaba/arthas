package com.taobao.arthas.core.security;

import java.security.Principal;

/**
 * 本地连接的特殊处理 {@link Principal}
 * <p>
 * 该类实现了Java安全框架中的Principal接口，用于标识本地连接的特殊身份。
 * 本地连接通常指从本机发起的连接，这类连接通常不需要经过完整的认证流程。
 * </p>
 * <p>
 * 该类的主要用途：</p>
 * <ul>
 * <li>作为本地连接的身份标识，区别于远程连接</li>
 * <li>在安全认证流程中标识本地连接，可以绕过某些安全检查</li>
 * <li>提供统一的接口与Java安全框架集成</li>
 * </ul>
 *
 * @author hengyunabc 2021-09-01
 */
public final class LocalConnectionPrincipal implements Principal {

    /**
     * 构造一个本地连接主体对象
     * <p>
     * 无参构造函数，创建一个空的本地连接主体实例。
     * 本地连接主体不需要用户名、密码等认证信息。
     * </p>
     */
    public LocalConnectionPrincipal() {
    }

    /**
     * 获取主体的名称
     * <p>
     * Principal接口的标准方法，用于获取主体名称。
     * 对于本地连接，该方法返回null，因为本地连接不需要特定的名称标识。
     * </p>
     *
     * @return 始终返回null
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * 获取用户名
     * <p>
     * 返回本地连接的用户名。
     * 对于本地连接，该方法返回null，因为本地连接通常不需要用户名。
     * </p>
     *
     * @return 始终返回null
     */
    public String getUsername() {
        return null;
    }

    /**
     * 获取密码
     * <p>
     * 返回本地连接的密码。
     * 对于本地连接，该方法返回null，因为本地连接通常不需要密码认证。
     * </p>
     *
     * @return 始终返回null
     */
    public String getPassword() {
        return null;
    }
}