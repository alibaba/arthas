package com.taobao.arthas.core.command.express;

import ognl.MemberAccess;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * 默认成员访问控制
 *
 * 该类实现了OGNL的MemberAccess接口，用于控制对类成员（字段、方法、构造函数）的访问权限。
 * 它提供了粗粒度的访问控制，允许在OGNL表达式中访问private、protected和package-private成员。
 *
 * <p>主要功能：</p>
 * <ul>
 * <li>动态设置和恢复成员的可访问性</li>
 * <li>根据配置决定是否允许访问非public成员</li>
 * <li>支持对private、protected、package-private成员的细粒度控制</li>
 * <li>通过反射机制绕过Java的访问控制检查</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * 在Arthas的OGNL表达式求值过程中，需要访问被观察对象的私有成员进行诊断和分析。
 *
 * <p>安全说明：</p>
 * 该类会临时修改Java的访问控制，应谨慎使用，仅在受控的诊断环境中使用。
 *
 * 基于ognl.DefaultMemberAccess (ognl:ognl:3.1.19)实现
 *
 * This class provides methods for setting up and restoring
 * access in a Field.  Java 2 provides access utilities for setting
 * and getting fields that are non-public.  This object provides
 * coarse-grained access controls to allow access to private, protected
 * and package protected members.  This will apply to all classes
 * and members.
 */
public class DefaultMemberAccess implements MemberAccess {

    /**
     * 是否允许访问private成员
     * 默认为false，不允许访问私有成员
     */
    public boolean allowPrivateAccess = false;

    /**
     * 是否允许访问protected成员
     * 默认为false，不允许访问受保护成员
     */
    public boolean allowProtectedAccess = false;

    /**
     * 是否允许访问package-private（默认访问权限）成员
     * 默认为false，不允许访问包私有成员
     */
    public boolean allowPackageProtectedAccess = false;

    /**
     * 构造函数：统一设置所有访问权限
     *
     * @param allowAllAccess 是否允许访问所有非public成员（private、protected、package-private）
     */
    public DefaultMemberAccess(boolean allowAllAccess) {
        // 调用完整构造函数，将所有访问权限设置为相同值
        this(allowAllAccess, allowAllAccess, allowAllAccess);
    }

    /**
     * 构造函数：分别设置各类型成员的访问权限
     *
     * @param allowPrivateAccess 是否允许访问private成员
     * @param allowProtectedAccess 是否允许访问protected成员
     * @param allowPackageProtectedAccess 是否允许访问package-private成员
     */
    public DefaultMemberAccess(boolean allowPrivateAccess, boolean allowProtectedAccess, boolean allowPackageProtectedAccess) {
        super();
        this.allowPrivateAccess = allowPrivateAccess;
        this.allowProtectedAccess = allowProtectedAccess;
        this.allowPackageProtectedAccess = allowPackageProtectedAccess;
    }

    /**
     * 获取是否允许访问private成员
     *
     * @return 如果允许访问private成员返回true，否则返回false
     */
    public boolean getAllowPrivateAccess() {
        return allowPrivateAccess;
    }

    /**
     * 设置是否允许访问private成员
     *
     * @param value true表示允许访问，false表示不允许访问
     */
    public void setAllowPrivateAccess(boolean value) {
        allowPrivateAccess = value;
    }

    /**
     * 获取是否允许访问protected成员
     *
     * @return 如果允许访问protected成员返回true，否则返回false
     */
    public boolean getAllowProtectedAccess() {
        return allowProtectedAccess;
    }

    /**
     * 设置是否允许访问protected成员
     *
     * @param value true表示允许访问，false表示不允许访问
     */
    public void setAllowProtectedAccess(boolean value) {
        allowProtectedAccess = value;
    }

    /**
     * 获取是否允许访问package-private成员
     *
     * @return 如果允许访问package-private成员返回true，否则返回false
     */
    public boolean getAllowPackageProtectedAccess() {
        return allowPackageProtectedAccess;
    }

    /**
     * 设置是否允许访问package-private成员
     *
     * @param value true表示允许访问，false表示不允许访问
     */
    public void setAllowPackageProtectedAccess(boolean value) {
        allowPackageProtectedAccess = value;
    }

    /**
     * 设置成员的可访问性
     *
     * 在访问成员之前调用此方法，如果成员具有访问权限且当前不可访问，
     * 则使用反射的setAccessible(true)方法临时覆盖Java的访问控制检查。
     *
     * @param context OGNL表达式上下文（当前未使用）
     * @param target 目标对象，包含要访问的成员（当前未使用）
     * @param member 要访问的成员（字段、方法或构造函数）
     * @param propertyName 属性名称（当前未使用）
     * @return 如果成员的访问状态被修改，返回Boolean.TRUE；否则返回null
     */
    @Override
    public Object setup(Map context, Object target, Member member, String propertyName) {
        // 结果对象，用于存储成员的原始访问状态
        Object result = null;

        // 检查是否允许访问该成员
        if (isAccessible(context, target, member, propertyName)) {
            // 将Member转换为AccessibleObject以调用setAccessible方法
            AccessibleObject accessible = (AccessibleObject) member;

            // 如果成员当前不可访问，则临时设置为可访问
            if (!accessible.isAccessible()) {
                // 记录原始状态为不可访问（true表示需要恢复）
                result = Boolean.TRUE;
                // 绕过Java的访问控制检查，设置为可访问
                accessible.setAccessible(true);
            }
        }
        return result;
    }

    /**
     * 恢复成员的可访问性
     *
     * 在访问成员完成后调用此方法，恢复成员的原始访问状态。
     * 这是一个清理方法，确保不会长期影响Java的安全性。
     *
     * @param context OGNL表达式上下文（当前未使用）
     * @param target 目标对象（当前未使用）
     * @param member 被访问的成员
     * @param propertyName 属性名称（当前未使用）
     * @param state setup方法返回的状态对象，如果不是null则表示需要恢复访问性
     */
    @Override
    public void restore(Map context, Object target, Member member, String propertyName, Object state) {
        // 如果状态不为null，说明成员的访问性被修改过，需要恢复
        if (state != null) {
            // 恢复成员的原始访问状态（false表示不可访问）
            ((AccessibleObject)member).setAccessible((Boolean)state);
        }
    }

    /**
     * 判断给定成员是否可访问
     *
     * 根据成员的修饰符和当前配置的访问权限，判断是否允许访问该成员。
     * 访问规则：
     * 1. public成员始终可访问
     * 2. private成员根据allowPrivateAccess配置决定
     * 3. protected成员根据allowProtectedAccess配置决定
     * 4. package-private成员根据allowPackageProtectedAccess配置决定
     *
     * Returns true if the given member is accessible or can be made accessible
     * by this object.
     *
     * @param context OGNL表达式上下文（当前未使用）
     * @param target 目标对象（当前未使用）
     * @param member 要检查访问权限的成员
     * @param propertyName 属性名称（当前未使用）
     * @return 如果成员在当前上下文中可访问返回true，否则返回false
     */
    @Override
    public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
        // 获取成员的修饰符
        int modifiers = member.getModifiers();

        // 默认结果：public成员始终可访问
        boolean result = Modifier.isPublic(modifiers);

        // 如果不是public成员，检查其他访问权限
        if (!result) {
            // 检查是否为private成员
            if (Modifier.isPrivate(modifiers)) {
                // 根据配置决定是否允许访问private成员
                result = getAllowPrivateAccess();
            } else {
                // 检查是否为protected成员
                if (Modifier.isProtected(modifiers)) {
                    // 根据配置决定是否允许访问protected成员
                    result = getAllowProtectedAccess();
                } else {
                    // 剩余情况为package-private（默认访问权限）
                    // 根据配置决定是否允许访问package-private成员
                    result = getAllowPackageProtectedAccess();
                }
            }
        }
        return result;
    }
}