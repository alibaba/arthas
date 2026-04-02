package com.taobao.arthas.core.security;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.util.StringUtils;

/**
 * 安全认证器实现类
 *
 * 该类实现了基本的用户名密码认证功能，支持多种认证方式：
 * 1. Basic认证：基于用户名和密码的认证
 * 2. Bearer Token认证：基于Token的认证
 * 3. 本地连接认证：本地连接自动通过认证
 *
 * TODO 支持不同角色不同权限，command按角色分类？
 *
 * @author hengyunabc 2021-03-03
 */
public class SecurityAuthenticatorImpl implements SecurityAuthenticator {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuthenticatorImpl.class);

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 主体对象
     * 登录成功后返回的Subject对象
     */
    private Subject subject;

    /**
     * 构造函数
     *
     * @param username 用户名，可以为null
     * @param password 密码，可以为null
     *
     * 构造函数的特殊处理逻辑：
     * - 如果用户名不为null但密码为null，则自动生成一个32位的随机密码并打印到日志
     * - 如果用户名为null但密码不为null，则使用默认用户名
     * - 创建一个新的Subject对象用于认证
     */
    public SecurityAuthenticatorImpl(String username, String password) {
        // 如果提供了用户名但未提供密码，自动生成一个随机密码
        if (username != null && password == null) {
            password = StringUtils.randomString(32);
            logger.info("\nUsing generated security password: {}\n", password);
        }
        // 如果未提供用户名但提供了密码，使用默认用户名
        if (username == null && password != null) {
            username = ArthasConstants.DEFAULT_USERNAME;
        }

        this.username = username;
        this.password = password;

        // 创建一个新的Subject对象
        subject = new Subject();
    }

    /**
     * 设置认证领域名称
     *
     * @param name 认证领域名称
     *
     * TODO 待实现
     */
    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    /**
     * 获取认证领域名称
     *
     * @return 认证领域名称
     *
     * TODO 待实现
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 设置角色类名
     *
     * @param names 角色类名（多个用逗号分隔）
     *
     * TODO 待实现
     */
    @Override
    public void setRoleClassNames(String names) {
        // TODO Auto-generated method stub

    }

    /**
     * 登录认证
     *
     * 支持三种认证方式：
     * 1. BasicPrincipal：使用用户名和密码进行Basic认证
     * 2. BearerPrincipal：使用Token进行Bearer认证
     * 3. LocalConnectionPrincipal：本地连接直接通过认证
     *
     * @param principal 要认证的主体对象
     * @return 认证成功返回Subject对象，认证失败或principal为null返回null
     * @throws LoginException 如果登录过程中发生错误
     */
    @Override
    public Subject login(Principal principal) throws LoginException {
        // 如果principal为null，返回null
        if (principal == null) {
            return null;
        }

        // Basic认证：检查用户名和密码
        if (principal instanceof BasicPrincipal) {
            BasicPrincipal basicPrincipal = (BasicPrincipal) principal;
            // 验证用户名和密码是否匹配
            if (basicPrincipal.getName().equals(username) && basicPrincipal.getPassword().equals(this.password)) {
                return subject;
            }
        }

        // Bearer Token认证：检查Token是否匹配密码
        if (principal instanceof BearerPrincipal) {
            BearerPrincipal bearerPrincipal = (BearerPrincipal) principal;
            // Bearer Token认证：将token作为password进行验证
            if (bearerPrincipal.getToken().equals(this.password)) {
                return subject;
            }
        }

        // 本地连接认证：本地连接直接通过
        if (principal instanceof LocalConnectionPrincipal) {
            return subject;
        }

        // 认证失败，返回null
        return null;
    }

    /**
     * 登出
     *
     * @param subject 要登出的主体对象
     * @throws LoginException 如果登出过程中发生错误
     *
     * TODO 待实现
     */
    @Override
    public void logout(Subject subject) throws LoginException {
        // TODO Auto-generated method stub

    }

    /**
     * 获取用户角色
     *
     * @param subject 主体对象
     * @return 角色字符串
     *
     * TODO 待实现
     */
    @Override
    public String getUserRoles(Subject subject) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 判断是否需要登录
     *
     * @return 如果用户名和密码都不为null则返回true，否则返回false
     *         这意味着只有当同时设置了用户名和密码时才需要认证
     */
    @Override
    public boolean needLogin() {
        return username != null && password != null;
    }

}
