package com.taobao.arthas.core.shell.system.impl;

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

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 全局的Job Controller，不应该存在启停的概念，不需要在连接的断开时关闭，
 * 
 * @author gehui 2017年7月31日 上午11:55:41
 */
public class GlobalJobControllerImpl extends JobControllerImpl {
    private Map<Integer, TimerTask> jobTimeoutTaskMap = new HashMap<Integer, TimerTask>();
    private static final Logger logger = LoggerFactory.getLogger(GlobalJobControllerImpl.class);

    @Override
    public void close(final Handler<Void> completionHandler) {
        if (completionHandler != null) {
            completionHandler.handle(null);
        }
    }

    @Override
    public void close() {
        jobTimeoutTaskMap.clear();
        for (Job job : jobs()) {
            job.terminate();
        }
    }

    @Override
    public boolean removeJob(int id) {
        TimerTask jobTimeoutTask = jobTimeoutTaskMap.remove(id);
        if (jobTimeoutTask != null) {
            jobTimeoutTask.cancel();
        }
        return super.removeJob(id);
    }

    @Override
    public Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, Session session, JobListener jobHandler, Term term, ResultDistributor resultDistributor) {
        final Job job = super.createJob(commandManager, tokens, session, jobHandler, term, resultDistributor);

        /*
         * 达到超时时间将会停止job
         */
        TimerTask jobTimeoutTask = new JobTimeoutTask(job);
        Date timeoutDate = new Date(System.currentTimeMillis() + (getJobTimeoutInSecond() * 1000));
        ArthasBootstrap.getInstance().getTimer().schedule(jobTimeoutTask, timeoutDate);
        jobTimeoutTaskMap.put(job.id(), jobTimeoutTask);
        job.setTimeoutDate(timeoutDate);

        return job;
    }

    private long getJobTimeoutInSecond() {
        long result = -1;
        String jobTimeoutConfig = GlobalOptions.jobTimeout.trim();
        try {
            char unit = jobTimeoutConfig.charAt(jobTimeoutConfig.length() - 1);
            String duration = jobTimeoutConfig.substring(0, jobTimeoutConfig.length() - 1);
            switch (unit) {
            case 'h':
                result = TimeUnit.HOURS.toSeconds(Long.parseLong(duration));
                break;
            case 'd':
                result = TimeUnit.DAYS.toSeconds(Long.parseLong(duration));
                break;
            case 'm':
                result = TimeUnit.MINUTES.toSeconds(Long.parseLong(duration));
                break;
            case 's':
                result = Long.parseLong(duration);
                break;
            default:
                result = Long.parseLong(jobTimeoutConfig);
                break;
            }
        } catch (Exception e) {
        }

        if (result < 0) {
            // 如果设置的属性有错误，那么使用默认的1天
            result = TimeUnit.DAYS.toSeconds(1);
            logger.warn("Configuration with job timeout " + jobTimeoutConfig + " is error, use 1d in default.");
        }
        return result;
    }

    private static class JobTimeoutTask extends TimerTask {
        Job job;

        public JobTimeoutTask(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            if (job != null) {
                job.terminate();
            }
        }

        @Override
        public boolean cancel() {
            // clear job reference from timer
            // fix issue: https://github.com/alibaba/arthas/issues/1189
            job = null;
            return super.cancel();
        }
    }
}
