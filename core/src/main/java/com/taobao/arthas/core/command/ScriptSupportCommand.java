package com.taobao.arthas.core.command;

import com.taobao.arthas.core.advisor.Advice;

/**
 * 脚本支持命令
 * Created by vlinux on 15/6/1.
 */
public interface ScriptSupportCommand {

    /**
     * 增强脚本监听器
     */
    interface ScriptListener {

        /**
         * 脚本创建
         *
         * @param output 输出器
         */
        void create(Output output);

        /**
         * 脚本销毁
         *
         * @param output 输出器
         */
        void destroy(Output output);

        /**
         * 方法执行前
         *
         * @param output 输出器
         * @param advice 通知点
         */
        void before(Output output, Advice advice);

        /**
         * 方法正常返回
         *
         * @param output 输出器
         * @param advice 通知点
         */
        void afterReturning(Output output, Advice advice);

        /**
         * 方法异常返回
         *
         * @param output 输出器
         * @param advice 通知点
         */
        void afterThrowing(Output output, Advice advice);

    }

    /**
     * 脚本监听器适配器
     */
    class ScriptListenerAdapter implements ScriptListener {

        @Override
        public void create(Output output) {

        }

        @Override
        public void destroy(Output output) {

        }

        @Override
        public void before(Output output, Advice advice) {

        }

        @Override
        public void afterReturning(Output output, Advice advice) {

        }

        @Override
        public void afterThrowing(Output output, Advice advice) {

        }
    }


    /**
     * 输出器
     */
    interface Output {

        /**
         * 输出字符串(不换行)
         *
         * @param string 待输出字符串
         * @return this
         */
        Output print(String string);

        /**
         * 输出字符串(换行)
         *
         * @param string 待输出字符串
         * @return this
         */
        Output println(String string);

        /**
         * 结束当前脚本
         *
         * @return this
         */
        Output finish();

    }

}
