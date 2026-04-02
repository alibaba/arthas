package com.taobao.arthas.core.shell.term.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.session.Session;

import io.termd.core.readline.Function;
import io.termd.core.readline.Readline;
import io.termd.core.readline.Readline.Interaction;

/**
 * 函数调用拦截处理器
 *
 * 该类实现了Java动态代理的InvocationHandler接口，用于拦截termd库中Function的apply方法调用
 * 主要用于实现访问控制：在用户未认证时，阻止某些敏感功能的执行
 *
 * 工作原理：
 * 1. 通过动态代理包装原始的Function对象
 * 2. 当调用apply方法时，检查当前会话是否已认证
 * 3. 如果已认证，则正常执行原始功能
 * 4. 如果未认证，则直接返回，不执行原始功能
 *
 * 拦截指定的 Function 的 apply 函数
 *
 * @author hengyunabc 2023-08-24
 *
 */
public class FunctionInvocationHandler implements InvocationHandler {

    // 终端实现对象，用于访问当前会话信息
    private TermImpl termImpl;

    // 被代理的目标Function对象，即实际要执行的功能
    private Function target;

    /**
     * 构造函数调用拦截处理器
     *
     * @param termImpl 终端实现对象，用于访问当前会话信息
     * @param target 被代理的目标Function对象
     */
    public FunctionInvocationHandler(TermImpl termImpl, Function target) {
        this.termImpl = termImpl;
        this.target = target;
    }

    /**
     * 拦截方法调用
     *
     * 该方法是InvocationHandler接口的实现，用于拦截对代理对象的所有方法调用
     * 主要拦截apply方法，进行权限检查：
     * 1. 如果调用的是apply方法，检查用户是否已认证
     * 2. 如果已认证，正常执行原始功能
     * 3. 如果未认证，恢复交互状态并返回null，阻止功能执行
     * 4. 对于其他方法，直接调用原始对象的方法
     *
     * @param proxy 代理对象本身
     * @param method 被调用的方法对象
     * @param args 方法调用的参数数组
     * @return 方法调用的返回值，如果未认证则返回null
     * @throws Throwable 方法调用可能抛出的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 获取被调用方法的名称
        String name = method.getName();

        // 如果调用的是apply方法，则进行权限检查
        if (name.equals("apply")) {
            // 获取当前会话对象
            Session session = termImpl.getSession();
            // 如果会话存在，则检查是否已认证
            if (session != null) {
                // 通过检查会话中是否存在Subject来判断用户是否已认证
                boolean authenticated = session.get(ArthasConstants.SUBJECT_KEY) != null;
                // 如果用户已认证，则正常执行原始功能
                if (authenticated) {
                    return method.invoke(target, args);
                } else {
                    // 如果用户未认证，则恢复交互状态并阻止功能执行
                    Readline.Interaction interaction = (Interaction) args[0];
                    // 必要
                    interaction.resume();
                    // 返回null，不执行原始功能
                    return null;
                }
            }
        }

        // 对于非apply方法，直接调用原始对象的方法
        return method.invoke(target, args);
    }

}
