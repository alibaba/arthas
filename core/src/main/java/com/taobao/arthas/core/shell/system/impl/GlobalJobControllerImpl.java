package com.taobao.arthas.core.shell.system.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.term.Term;


/**
 * 全局作业控制器实现类
 *
 * 该类继承自JobControllerImpl，是Arthas中用于管理所有作业的全局控制器。
 * 与普通的JobController相比，它具有以下特点：
 * 1. 不存在启动和停止的概念，是一个长期运行的控制器
 * 2. 不会在连接断开时关闭，独立于任何特定连接的生命周期
 * 3. 支持作业超时管理，可以为每个作业设置超时时间
 * 4. 管理所有Arthas会话中创建的作业
 *
 * 主要功能：
 * - 创建和管理所有作业
 * - 监控作业执行时间，超时自动终止
 * - 支持配置化的超时时间（支持小时、天、分钟、秒等单位）
 * - 清理和终止作业
 *
 * @author gehui 2017年7月31日 上午11:55:41
 */
public class GlobalJobControllerImpl extends JobControllerImpl {

    /**
     * 作业超时任务映射表
     * key: 作业ID
     * value: 对应的超时任务
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<Integer, JobTimeoutTask> jobTimeoutTaskMap = new ConcurrentHashMap<Integer, JobTimeoutTask>();

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(GlobalJobControllerImpl.class);

    /**
     * 关闭作业控制器
     * 由于这是全局控制器，实际上不应该被关闭
     * 该方法只是简单地调用完成处理器，不做任何实际清理工作
     *
     * @param completionHandler 完成处理器，用于在关闭操作完成后执行回调
     */
    @Override
    public void close(final Handler<Void> completionHandler) {
        // 如果提供了完成处理器，则调用它
        if (completionHandler != null) {
            completionHandler.handle(null);
        }
    }

    /**
     * 关闭作业控制器
     * 执行实际的清理工作：
     * 1. 清除所有超时任务
     * 2. 终止所有正在运行的作业
     * 注意：这个方法通常只在Arthas完全关闭时调用
     */
    @Override
    public void close() {
        // 清除所有超时任务映射
        jobTimeoutTaskMap.clear();

        // 终止所有作业
        for (Job job : jobs()) {
            job.terminate();
        }
    }

    /**
     * 移除指定的作业
     * 在移除作业的同时，也会取消对应的超时任务
     *
     * @param id 要移除的作业ID
     * @return 如果作业存在并成功移除返回true，否则返回false
     */
    @Override
    public boolean removeJob(int id) {
        // 从超时任务映射中移除对应的任务
        JobTimeoutTask jobTimeoutTask = jobTimeoutTaskMap.remove(id);

        // 如果存在超时任务，则取消它
        if (jobTimeoutTask != null) {
            jobTimeoutTask.cancel();
        }

        // 调用父类方法移除作业
        return super.removeJob(id);
    }

    /**
     * 创建一个新的作业
     * 在创建作业的同时，还会：
     * 1. 为作业创建超时任务
     * 2. 计算超时时间
     * 3. 设置作业的超时时间
     * 4. 将超时任务注册到调度器中
     *
     * @param commandManager 内部命令管理器，用于管理命令的执行
     * @param tokens 命令行令牌列表，包含解析后的命令和参数
     * @param session 会话对象，包含当前会话的上下文信息
     * @param jobHandler 作业监听器，用于监听作业的生命周期事件
     * @param term 终端对象，用于与用户交互
     * @param resultDistributor 结果分发器，用于分发命令执行结果
     * @return 创建的作业对象
     */
    @Override
    public Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, Session session, JobListener jobHandler, Term term, ResultDistributor resultDistributor) {
        // 调用父类方法创建作业对象
        final Job job = super.createJob(commandManager, tokens, session, jobHandler, term, resultDistributor);

        /*
         * 为作业设置超时机制
         * 当达到超时时间后，作业将被自动终止
         * 这可以防止长时间运行的命令占用资源
         */
        JobTimeoutTask jobTimeoutTask = new JobTimeoutTask(job);

        // 获取配置的超时时间（秒）
        long jobTimeoutInSecond = getJobTimeoutInSecond();

        // 计算超时的具体时间点
        Date timeoutDate = new Date(System.currentTimeMillis() + (jobTimeoutInSecond * 1000));

        // 将超时任务调度到定时执行器中
        ArthasBootstrap.getInstance().getScheduledExecutorService().schedule(jobTimeoutTask, jobTimeoutInSecond, TimeUnit.SECONDS);

        // 将超时任务保存到映射表中，以便后续取消
        jobTimeoutTaskMap.put(job.id(), jobTimeoutTask);

        // 设置作业的超时时间
        job.setTimeoutDate(timeoutDate);

        return job;
    }

    /**
     * 获取作业超时配置的秒数
     * 从全局配置中读取超时时间，并解析为秒数
     *
     * 支持的时间单位：
     * - h: 小时（如：1h, 24h）
     * - d: 天（如：1d, 7d）
     * - m: 分钟（如：30m, 60m）
     * - s: 秒（如：60s, 120s）
     * - 无单位：默认为秒（如：60）
     *
     * 如果配置解析失败，则使用默认值1天
     *
     * @return 超时时间的秒数
     */
    private long getJobTimeoutInSecond() {
        long result = -1;

        // 获取配置的超时时间字符串
        String jobTimeoutConfig = GlobalOptions.jobTimeout.trim();

        try {
            // 获取最后一个字符作为单位
            char unit = jobTimeoutConfig.charAt(jobTimeoutConfig.length() - 1);

            // 获取除了单位之外的数字部分
            String duration = jobTimeoutConfig.substring(0, jobTimeoutConfig.length() - 1);

            // 根据单位进行时间转换
            switch (unit) {
                case 'h':
                    // 小时转换为秒
                    result = TimeUnit.HOURS.toSeconds(Long.parseLong(duration));
                    break;
                case 'd':
                    // 天转换为秒
                    result = TimeUnit.DAYS.toSeconds(Long.parseLong(duration));
                    break;
                case 'm':
                    // 分钟转换为秒
                    result = TimeUnit.MINUTES.toSeconds(Long.parseLong(duration));
                    break;
                case 's':
                    // 秒不需要转换
                    result = Long.parseLong(duration);
                    break;
                default:
                    // 如果没有单位或单位不识别，尝试直接解析为秒数
                    result = Long.parseLong(jobTimeoutConfig);
                    break;
            }
        } catch (Throwable e) {
            // 解析出错，记录错误日志
            logger.error("parse jobTimeoutConfig: {} error!", jobTimeoutConfig, e);
        }

        // 如果解析结果无效（小于0），使用默认值1天
        if (result < 0) {
            result = TimeUnit.DAYS.toSeconds(1);
            logger.warn("Configuration with job timeout " + jobTimeoutConfig + " is error, use 1d in default.");
        }

        return result;
    }

    /**
     * 作业超时任务
     * 这是一个可运行的定时任务，用于在作业超时时自动终止作业
     *
     * 设计特点：
     * - 使用volatile风格的null检查来取消任务（通过将job设置为null）
     * - 执行时会将job临时保存并置空，防止重复执行
     * - 异常处理完善，确保不会因为错误影响系统稳定性
     */
    private static class JobTimeoutTask implements Runnable {

        /**
         * 要监控的作业
         * 当设置为null时，表示任务已取消
         */
        private Job job;

        /**
         * 构造函数
         *
         * @param job 要监控超时的作业对象
         */
        public JobTimeoutTask(Job job) {
            this.job = job;
        }

        /**
         * 任务执行方法
         * 当作业超时时，此方法会被调用，终止作业的执行
         */
        @Override
        public void run() {
            try {
                // 检查作业是否还存在（没有被取消）
                if (job != null) {
                    // 临时保存作业引用，然后立即置空
                    // 这样可以防止任务被执行多次
                    Job temp = job;
                    job = null;

                    // 终止作业
                    temp.terminate();
                }
            } catch (Throwable e) {
                // 如果终止作业时出错，记录错误日志
                try {
                    logger.error("JobTimeoutTask error, job id: {}, line: {}", job.id(), job.line(), e);
                } catch (Throwable t) {
                    // 如果记录日志也出错（比如job已为null），则忽略
                    // ignore
                }
            }
        }

        /**
         * 取消超时任务
         * 通过将job设置为null来标记任务已取消
         * 这样即使任务被调度执行，也不会实际终止作业
         */
        public void cancel() {
            job = null;
        }
    }
}
