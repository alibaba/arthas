package com.taobao.arthas.core.security;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * 安全认证器接口
 *
 * 一个 {@link SecurityAuthenticator} 允许插入自定义认证器，
 * 例如基于JAAS的认证器或自定义实现的认证器。
 *
 * 该接口定义了用户认证所需的基本方法，包括登录、登出、
 * 角色管理等核心功能。实现类可以根据实际需求提供不同的
 * 认证机制，如用户名密码认证、Token认证等。
 *
 * @author hengyunabc
 * @see Subject
 * @see Principal
 * @see LoginException
 */
public interface SecurityAuthenticator {


    /**
     * 判断是否需要登录
     *
     * @return 如果需要登录返回true，否则返回false
     */
    boolean needLogin();

    /**
     * 设置认证领域的名称
     *
     * @param name 认证领域的名称
     */
    void setName(String name);

    /**
     * 获取认证领域的名称
     *
     * @return 认证领域的名称
     */
    String getName();

    /**
     * 设置角色类名（多个类名用逗号分隔）
     * <p/>
     * 如果没有显式配置角色类名，默认实现会假设
     * {@link Subject} 的 {@link java.security.Principal} 中
     * 类名包含单词 <tt>role</tt>（小写）的为主体角色。
     *
     * @param names 角色 {@link java.security.Principal} 实现类的
     *              完全限定类名列表，多个类名用逗号分隔
     */
    void setRoleClassNames(String names);

    /**
     * 尝试在该认证领域登录 {@link java.security.Principal}
     * <p/>
     * 如果没有抛出异常且返回了非空的 {@link Subject}，则表示登录成功。
     *
     * @param principal 要登录的主体对象
     * @return 登录成功的主体对象，必须 <b>不为</b> <tt>null</tt>
     * @throws LoginException 如果登录 {@link java.security.Principal} 时发生错误
     */
    Subject login(Principal principal) throws LoginException;

    /**
     * 尝试登出主体
     *
     * @param subject 要登出的主体对象
     * @throws LoginException 如果登出主体时发生错误
     */
    void logout(Subject subject) throws LoginException;

    /**
     * 从给定的 {@link Subject} 中获取用户角色
     *
     * @param subject 主体对象
     * @return 如果没有角色则返回 <tt>null</tt>，否则返回用逗号分隔的角色字符串
     */
    String getUserRoles(Subject subject);

}
