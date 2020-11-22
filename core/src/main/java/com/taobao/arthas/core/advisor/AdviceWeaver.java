package com.taobao.arthas.core.advisor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 * 通知编织者<br/>
 * <p/>
 * <h2>线程帧栈与执行帧栈</h2>
 * 编织者在执行通知的时候有两个重要的栈:线程帧栈(threadFrameStack),执行帧栈(frameStack)
 * <p/>
 * Created by vlinux on 15/5/17.
 */
public class AdviceWeaver {

    private static final Logger logger = LoggerFactory.getLogger(AdviceWeaver.class);

    // 通知监听器集合
    private final static Map<Long/*ADVICE_ID*/, AdviceListener> advices
            = new ConcurrentHashMap<Long, AdviceListener>();

    /**
     * 注册监听器
     *
     * @param adviceId 通知ID
     * @param listener 通知监听器
     */
    public static void reg(AdviceListener listener) {

        // 触发监听器创建
        listener.create();

        // 注册监听器
        advices.put(listener.id(), listener);
    }

    /**
     * 注销监听器
     *
     * @param adviceId 通知ID
     */
    public static void unReg(AdviceListener listener) {
        if (null != listener) {
            // 注销监听器
            advices.remove(listener.id());

            // 触发监听器销毁
            listener.destroy();
        }
    }

    public static AdviceListener listener(long id) {
        return advices.get(id);
    }

    /**
     * 恢复监听
     *
     * @param listener 通知监听器
     */
    public static void resume(AdviceListener listener) {
        // 注册监听器
        advices.put(listener.id(), listener);
    }

    /**
     * 暂停监听
     *
     * @param adviceId 通知ID
     */
    public static AdviceListener suspend(long adviceId) {
        // 注销监听器
        return advices.remove(adviceId);
    }

}
