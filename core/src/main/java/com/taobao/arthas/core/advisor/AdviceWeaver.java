package com.taobao.arthas.core.advisor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 * 通知编织者（Advice Weaver）
 *
 * 该类是Arthas方法增强机制的核心组件，负责管理AdviceListener（通知监听器）的生命周期。
 * 它维护所有注册的监听器，并提供注册、注销、暂停、恢复等操作。
 *
 * <h2>核心概念</h2>
 * <ul>
 * <li>AdviceListener: 通知监听器，用于在方法执行的特定时机（before/return/throw）接收通知</li>
 * <li>Advice ID: 每个监听器都有唯一的ID，用于标识和查找</li>
 * <li>线程帧栈与执行帧栈: 编织者在执行通知时维护两个重要的栈结构</li>
 * </ul>
 *
 * <h2>主要功能</h2>
 * <ul>
 * <li>注册/注销监听器</li>
 * <li>暂停/恢复监听器</li>
 * <li>根据ID查询监听器</li>
 * <li>触发监听器的创建和销毁回调</li>
 * </ul>
 *
 * Created by vlinux on 15/5/17.
 */
public class AdviceWeaver {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(AdviceWeaver.class);

    /**
     * 通知监听器集合
     * 使用ConcurrentHashMap保证线程安全
     * Key: AdviceListener的唯一ID（Long类型）
     * Value: 对应的AdviceListener实例
     */
    private final static Map<Long/*ADVICE_ID*/, AdviceListener> advices
            = new ConcurrentHashMap<Long, AdviceListener>();

    /**
     * 注册监听器
     * 将新的AdviceListener注册到编织者中，使其能够接收方法增强的通知
     *
     * 执行流程：
     * 1. 调用监听器的create()方法，触发创建回调
     * 2. 将监听器放入advices映射表中，使用监听器的ID作为key
     *
     * @param listener 要注册的通知监听器，不能为null
     */
    public static void reg(AdviceListener listener) {

        // 触发监听器创建回调，让监听器有机会执行初始化逻辑
        listener.create();

        // 将监听器注册到映射表中，使用其ID作为key
        advices.put(listener.id(), listener);
    }

    /**
     * 注销监听器
     * 从编织者中移除指定的AdviceListener，停止其接收通知
     *
     * 执行流程：
     * 1. 从advices映射表中移除该监听器
     * 2. 调用监听器的destroy()方法，触发销毁回调
     *
     * @param listener 要注销的通知监听器，如果为null则不执行任何操作
     */
    public static void unReg(AdviceListener listener) {
        if (null != listener) {
            // 从映射表中移除该监听器
            advices.remove(listener.id());

            // 触发监听器销毁回调，让监听器有机会执行清理逻辑
            listener.destroy();
        }
    }

    /**
     * 根据ID查询监听器
     * 获取指定ID对应的AdviceListener实例
     *
     * @param id 监听器的唯一标识ID
     * @return 对应的AdviceListener实例，如果不存在则返回null
     */
    public static AdviceListener listener(long id) {
        return advices.get(id);
    }

    /**
     * 恢复监听
     * 将之前暂停的监听器重新注册，使其能够继续接收通知
     *
     * 使用场景：用户在使用watch等命令后，可以使用相关命令暂停监听，
     * 之后可以通过此方法恢复监听，而不需要重新创建监听器
     *
     * @param listener 要恢复的通知监听器
     */
    public static void resume(AdviceListener listener) {
        // 将监听器重新注册到映射表中
        advices.put(listener.id(), listener);
    }

    /**
     * 暂停监听
     * 从编织者中移除指定ID的监听器，但不触发销毁回调
     *
     * 与unReg的区别：
     * - suspend: 仅移除监听器，不触发destroy()，可以后续通过resume()恢复
     * - unReg: 移除监听器并触发destroy()，是永久性的注销
     *
     * 使用场景：用户想临时暂停某个监听器，但保留其状态以便后续恢复
     *
     * @param adviceId 要暂停的监听器ID
     * @return 被暂停的AdviceListener实例，如果不存在则返回null
     */
    public static AdviceListener suspend(long adviceId) {
        // 从映射表中移除该监听器，但不调用destroy()
        return advices.remove(adviceId);
    }

}
