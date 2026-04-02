package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.ScriptSupportCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Groovy脚本建议监听器
 *
 * 由于严重的内存泄漏问题，Arthas 3.0中完全放弃了对Groovy的支持。
 * 该类用于将Groovy脚本的事件转发到脚本监听器。
 *
 * @author beiwei30 on 01/12/2016.
 * @deprecated 已废弃，Groovy支持因内存泄漏问题被移除
 */
@Deprecated
public class GroovyAdviceListener extends AdviceListenerAdapter {

    // Groovy脚本监听器，负责处理脚本逻辑
    private ScriptSupportCommand.ScriptListener scriptListener;

    // 输出适配器，用于向命令进程输出结果
    private ScriptSupportCommand.Output output;

    /**
     * 构造函数
     *
     * @param scriptListener Groovy脚本监听器，用于处理脚本事件
     * @param process 命令进程，用于输出执行结果
     */
    public GroovyAdviceListener(ScriptSupportCommand.ScriptListener scriptListener, CommandProcess process) {
        this.scriptListener = scriptListener;
        // 创建命令进程适配器，将ScriptSupportCommand.Output接口适配到CommandProcess
        this.output = new CommandProcessAdaptor(process);
    }

    /**
     * 监听器创建时的回调
     * 当监听器被创建并注册时调用，通知脚本监听器初始化
     */
    @Override
    public void create() {
        scriptListener.create(output);
    }

    /**
     * 监听器销毁时的回调
     * 当监听器被移除时调用，通知脚本监听器清理资源
     */
    @Override
    public void destroy() {
        scriptListener.destroy(output);
    }

    /**
     * 方法执行前的回调
     * 在目标方法执行之前调用，用于在方法入口处注入Groovy脚本逻辑
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象（实例方法为实例对象，静态方法为null）
     * @param args 方法参数数组
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 创建方法执行前的Advice对象，并传递给脚本监听器处理
        scriptListener.before(output, Advice.newForBefore(loader, clazz, method, target, args));
    }

    /**
     * 方法正常返回后的回调
     * 在目标方法正常执行完毕后调用，用于处理方法返回值
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象（实例方法为实例对象，静态方法为null）
     * @param args 方法参数数组
     * @param returnObject 方法返回值
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        // 创建方法正常返回后的Advice对象，并传递给脚本监听器处理
        scriptListener.afterReturning(output, Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject));
    }

    /**
     * 方法抛出异常后的回调
     * 在目标方法执行抛出异常时调用，用于处理异常情况
     *
     * @param loader 类加载器
     * @param clazz 目标类
     * @param method 目标方法
     * @param target 目标对象（实例方法为实例对象，静态方法为null）
     * @param args 方法参数数组
     * @param throwable 抛出的异常对象
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        // 创建方法抛出异常后的Advice对象，并传递给脚本监听器处理
        scriptListener.afterThrowing(output, Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    /**
     * 命令进程适配器
     *
     * 内部静态类，用于将ScriptSupportCommand.Output接口适配到CommandProcess。
     * 这样Groovy脚本可以通过统一的Output接口向命令进程输出结果。
     */
    private static class CommandProcessAdaptor implements ScriptSupportCommand.Output {

        // 底层的命令进程对象
        private CommandProcess process;

        /**
         * 构造函数
         *
         * @param process 命令进程对象
         */
        public CommandProcessAdaptor(CommandProcess process) {
            this.process = process;
        }

        /**
         * 输出字符串（不换行）
         *
         * @param string 要输出的字符串
         * @return 当前Output对象，支持链式调用
         */
        @Override
        public ScriptSupportCommand.Output print(String string) {
            process.write(string);
            return this;
        }

        /**
         * 输出字符串并换行
         *
         * @param string 要输出的字符串
         * @return 当前Output对象，支持链式调用
         */
        @Override
        public ScriptSupportCommand.Output println(String string) {
            process.write(string).write("\n");
            return this;
        }

        /**
         * 结束命令处理
         * 标记命令执行完成，结束命令进程
         *
         * @return 当前Output对象，支持链式调用
         */
        @Override
        public ScriptSupportCommand.Output finish() {
            process.end();
            return this;
        }
    }
}
