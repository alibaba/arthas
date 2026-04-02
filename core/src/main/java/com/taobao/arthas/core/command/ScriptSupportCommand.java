package com.taobao.arthas.core.command;

import com.taobao.arthas.core.advisor.Advice;

/**
 * 脚本支持命令接口
 *
 * 定义了支持脚本执行的命令规范，包括脚本监听器和输出器接口
 * 用于增强命令的脚本化执行能力
 *
 * Created by vlinux on 15/6/1.
 */
public interface ScriptSupportCommand {

    /**
     * 增强脚本监听器接口
     *
     * 定义了脚本生命周期的各个阶段回调方法
     * 实现此接口可以监听和控制脚本的执行过程
     */
    interface ScriptListener {

        /**
         * 脚本创建回调
         *
         * 当脚本被创建时调用，可以在此进行初始化操作
         *
         * @param output 输出器，用于输出脚本执行结果
         */
        void create(Output output);

        /**
         * 脚本销毁回调
         *
         * 当脚本被销毁时调用，可以在此进行清理操作
         *
         * @param output 输出器，用于输出脚本执行结果
         */
        void destroy(Output output);

        /**
         * 方法执行前回调
         *
         * 在目标方法执行前调用，可以在此获取方法入参等信息
         *
         * @param output 输出器，用于输出脚本执行结果
         * @param advice 通知点，包含目标方法的上下文信息
         */
        void before(Output output, Advice advice);

        /**
         * 方法正常返回回调
         *
         * 在目标方法正常返回时调用，可以在此获取返回值等信息
         *
         * @param output 输出器，用于输出脚本执行结果
         * @param advice 通知点，包含目标方法的上下文信息
         */
        void afterReturning(Output output, Advice advice);

        /**
         * 方法异常返回回调
         *
         * 在目标方法抛出异常时调用，可以在此获取异常信息
         *
         * @param output 输出器，用于输出脚本执行结果
         * @param advice 通知点，包含目标方法的上下文信息
         */
        void afterThrowing(Output output, Advice advice);

    }

    /**
     * 脚本监听器适配器类
     *
     * 提供了ScriptListener接口的空实现
     * 使用者可以继承此类，只重写需要的方法，而不需要实现所有方法
     * 这是适配器模式的典型应用
     */
    class ScriptListenerAdapter implements ScriptListener {

        /**
         * 脚本创建回调（空实现）
         *
         * @param output 输出器
         */
        @Override
        public void create(Output output) {

        }

        /**
         * 脚本销毁回调（空实现）
         *
         * @param output 输出器
         */
        @Override
        public void destroy(Output output) {

        }

        /**
         * 方法执行前回调（空实现）
         *
         * @param output 输出器
         * @param advice 通知点
         */
        @Override
        public void before(Output output, Advice advice) {

        }

        /**
         * 方法正常返回回调（空实现）
         *
         * @param output 输出器
         * @param advice 通知点
         */
        @Override
        public void afterReturning(Output output, Advice advice) {

        }

        /**
         * 方法异常返回回调（空实现）
         *
         * @param output 输出器
         * @param advice 通知点
         */
        @Override
        public void afterThrowing(Output output, Advice advice) {

        }
    }


    /**
     * 输出器接口
     *
     * 定义了脚本执行过程中的输出操作
     * 提供类似打印流的API，支持链式调用
     */
    interface Output {

        /**
         * 输出字符串（不换行）
         *
         * 将字符串输出到目标位置，不添加换行符
         *
         * @param string 待输出的字符串
         * @return 当前输出器实例，支持链式调用
         */
        Output print(String string);

        /**
         * 输出字符串（换行）
         *
         * 将字符串输出到目标位置，并添加换行符
         *
         * @param string 待输出的字符串
         * @return 当前输出器实例，支持链式调用
         */
        Output println(String string);

        /**
         * 结束当前脚本
         *
         * 标记脚本执行完成，执行必要的清理操作
         *
         * @return 当前输出器实例，支持链式调用
         */
        Output finish();

    }

}
