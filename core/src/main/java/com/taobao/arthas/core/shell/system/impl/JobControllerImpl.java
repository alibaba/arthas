package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.internal.RedirectHandler;
import com.taobao.arthas.core.shell.command.internal.StdoutHandler;
import com.taobao.arthas.core.shell.command.internal.TermHandler;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.impl.ProcessImpl.ProcessOutput;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.TokenUtils;

import io.termd.core.function.Function;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Job控制器实现类
 *
 * 负责管理和控制所有Job的生命周期，包括创建、运行、暂停、恢复和终止等操作
 * 支持命令管道、输出重定向等功能
 * 提供任务权限检查和安全管理
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-05-14
 * @author gongdewei 2020-03-23
 */
public class JobControllerImpl implements JobController {

    /**
     * 所有Job的映射表
     *
     * 使用TreeMap按Job ID排序存储，便于管理和查找
     * Key为Job ID，Value为JobImpl实例
     */
    private final SortedMap<Integer, JobImpl> jobs = new TreeMap<Integer, JobImpl>();

    /**
     * Job ID生成器
     *
     * 原子性递增，确保每个Job都有唯一的ID
     */
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * 控制器是否已关闭
     *
     * 关闭后不再接受新的Job创建请求
     */
    private boolean closed = false;

    /**
     * 构造函数
     */
    public JobControllerImpl() {
    }

    /**
     * 获取所有Job
     *
     * @return 所有Job的集合
     */
    public synchronized Set<Job> jobs() {
        return new HashSet<Job>(jobs.values());
    }

    /**
     * 根据ID获取Job
     *
     * @param id Job ID
     * @return 对应的Job对象，如果不存在则返回null
     */
    public synchronized Job getJob(int id) {
        return jobs.get(id);
    }

    /**
     * 移除指定ID的Job
     *
     * @param id Job ID
     * @return 如果成功移除返回true，如果Job不存在返回false
     */
    synchronized boolean removeJob(int id) {
        return jobs.remove(id) != null;
    }

    /**
     * 检查会话权限
     *
     * 如果启用了安全认证，检查会话是否已经通过认证
     * 未认证的会话只能执行auth命令，其他命令会被拒绝
     *
     * @param session 会话对象
     * @param token 命令token，用于判断是否为auth命令
     * @throws IllegalArgumentException 如果未认证且执行的不是auth命令
     */
    private void checkPermission(Session session, CliToken token) {
        // 检查是否需要登录认证
        if (ArthasBootstrap.getInstance().getSecurityAuthenticator().needLogin()) {
            // 检查session是否有 Subject（认证主体）
            Object subject = session.get(ArthasConstants.SUBJECT_KEY);
            if (subject == null) {
                // 如果没有认证，检查是否正在执行auth命令
                if (token != null && token.isText() && token.value().trim().equals(ArthasConstants.AUTH)) {
                    // 执行的是auth 命令，允许执行
                    return;
                }
                // 未认证且不是auth命令，抛出异常
                throw new IllegalArgumentException("Error! command not permitted, try to use 'auth' command to authenticates.");
            }
        }
    }

    /**
     * 创建一个新的Job
     *
     * 根据命令行tokens创建Job，包括：
     * 1. 生成唯一的Job ID
     * 2. 检查权限
     * 3. 解析命令行
     * 4. 创建进程
     * 5. 设置前台/后台运行模式
     *
     * @param commandManager 命令管理器，用于查找命令
     * @param tokens 命令行tokens列表
     * @param session 会话对象
     * @param jobHandler Job监听器
     * @param term 终端对象
     * @param resultDistributor 结果分发器
     * @return 创建的Job对象
     */
    @Override
    public Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, Session session, JobListener jobHandler, Term term, ResultDistributor resultDistributor) {
        // 检查权限
        checkPermission(session, tokens.get(0));
        // 生成唯一的Job ID
        int jobId = idGenerator.incrementAndGet();
        // 拼接完整的命令行字符串
        StringBuilder line = new StringBuilder();
        for (CliToken arg : tokens) {
            line.append(arg.raw());
        }
        // 检查是否为后台运行（命令以&结尾）
        boolean runInBackground = runInBackground(tokens);
        // 创建进程
        Process process = createProcess(session, tokens, commandManager, jobId, term, resultDistributor);
        process.setJobId(jobId);
        // 创建Job实例
        JobImpl job = new JobImpl(jobId, this, process, line.toString(), runInBackground, session, jobHandler);
        // 将Job加入到管理列表
        jobs.put(jobId, job);
        return job;
    }

    /**
     * 获取当前重定向到缓存的Job数量
     *
     * 统计所有进程中有缓存位置的Job数量
     * 用于限制同时进行的异步输出重定向任务数量
     *
     * @return 重定向到缓存的Job数量
     */
    private int getRedirectJobCount() {
        int count = 0;
        // 遍历所有Job，统计有缓存位置的Job数量
        for (Job job : jobs.values()) {
            if (job.process() != null && job.process().cacheLocation() != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 关闭Job控制器
     *
     * 终止所有正在运行的Job，并等待所有Job完全终止后调用完成处理器
     * 关闭后不再接受新的Job创建请求
     *
     * @param completionHandler 完成处理器，在所有Job终止后被调用
     */
    @Override
    public void close(final Handler<Void> completionHandler) {
        List<JobImpl> jobs;
        // 同步块：获取所有Job并标记控制器为已关闭
        synchronized (this) {
            if (closed) {
                // 如果已经关闭，返回空列表
                jobs = Collections.emptyList();
            } else {
                // 复制所有Job的列表
                jobs = new ArrayList<JobImpl>(this.jobs.values());
                // 标记控制器为已关闭
                closed = true;
            }
        }
        // 如果没有Job需要终止，直接调用完成处理器
        if (jobs.isEmpty()) {
            if (completionHandler!= null) {
                completionHandler.handle(null);
            }
        } else {
            // 使用计数器跟踪所有Job的终止状态
            final AtomicInteger count = new AtomicInteger(jobs.size());
            // 遍历所有Job，设置终止完成的回调
            for (JobImpl job : jobs) {
                job.terminateFuture.setHandler(new Handler<Future<Void>>() {
                    @Override
                    public void handle(Future<Void> v) {
                        // 每个Job终止后，计数器减1
                        // 当所有Job都终止后，调用完成处理器
                        if (count.decrementAndGet() == 0 && completionHandler != null) {
                            completionHandler.handle(null);
                        }
                    }
                });
                // 终止Job
                job.terminate();
            }
        }
    }

    /**
     * 从命令行tokens创建进程
     *
     * 解析命令行，找到对应的命令并创建进程
     * 支持权限检查，未认证的用户无法创建进程
     *
     * @param session 会话对象
     * @param line 命令行tokens列表
     * @param commandManager 命令管理器
     * @param jobId Job ID
     * @param term 终端对象
     * @param resultDistributor 结果分发器
     * @return 创建的进程对象
     * @throws RuntimeException 如果命令未找到或其他错误
     */
    private Process createProcess(Session session, List<CliToken> line, InternalCommandManager commandManager, int jobId, Term term, ResultDistributor resultDistributor) {
        try {
            // 使用列表迭代器遍历tokens
            ListIterator<CliToken> tokens = line.listIterator();
            while (tokens.hasNext()) {
                CliToken token = tokens.next();
                // 找到第一个文本类型的token（命令名）
                if (token.isText()) {
                    // 创建进程前检查权限
                    checkPermission(session, token);
                    // 从命令管理器获取命令对象
                    Command command = commandManager.getCommand(token.value());
                    if (command != null) {
                        // 创建命令进程
                        return createCommandProcess(command, tokens, jobId, term, resultDistributor);
                    } else {
                        // 命令未找到
                        throw new IllegalArgumentException(token.value() + ": command not found");
                    }
                }
            }
            // 没有找到有效的命令
            throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查命令是否应该在后台运行
     *
     * 如果命令行以&符号结尾，则表示后台运行
     * 同时会从tokens列表中移除&符号
     *
     * @param tokens 命令行tokens列表
     * @return 如果应该后台运行返回true，否则返回false
     */
    private boolean runInBackground(List<CliToken> tokens) {
        boolean runInBackground = false;
        // 查找最后一个文本类型的token
        CliToken last = TokenUtils.findLastTextToken(tokens);
        // 如果最后一个token是&，表示后台运行
        if (last != null && "&".equals(last.value())) {
            runInBackground = true;
            // 从tokens列表中移除&符号
            tokens.remove(last);
        }
        return runInBackground;
    }

    /**
     * 创建命令进程
     *
     * 解析命令参数，支持以下特性：
     * 1. 管道命令：使用|连接多个命令
     * 2. 输出重定向：使用>或>>将输出重定向到文件
     * 3. 自动保存结果：根据全局配置决定是否保存结果
     *
     * @param command 命令对象
     * @param tokens 命令参数tokens迭代器
     * @param jobId Job ID
     * @param term 终端对象
     * @param resultDistributor 结果分发器
     * @return 创建的进程对象
     * @throws IOException 如果发生I/O错误
     */
    private Process createCommandProcess(Command command, ListIterator<CliToken> tokens, int jobId, Term term, ResultDistributor resultDistributor) throws IOException {
        // 命令的剩余参数列表（不包括管道和重定向符号）
        List<CliToken> remaining = new ArrayList<CliToken>();
        // 管道命令的tokens列表
        List<CliToken> pipelineTokens = new ArrayList<CliToken>();
        // 是否为管道命令
        boolean isPipeline = false;
        // 重定向处理器
        RedirectHandler redirectHandler = null;
        // 标准输出处理链，支持多个处理器串联
        List<Function<String, String>> stdoutHandlerChain = new ArrayList<Function<String, String>>();
        // 缓存位置
        String cacheLocation = null;

        // 遍历剩余的tokens
        while (tokens.hasNext()) {
            CliToken remainingToken = tokens.next();
            if (remainingToken.isText()) {
                String tokenValue = remainingToken.value();
                // 检查管道符
                if ("|".equals(tokenValue)) {
                    isPipeline = true;
                    // 将管道符|之后的部分注入为输出链上的handler
                    injectHandler(stdoutHandlerChain, pipelineTokens);
                    continue;
                } else if (">>".equals(tokenValue) || ">".equals(tokenValue)) {
                    // 检查重定向符号
                    String name = getRedirectFileName(tokens);
                    if (name == null) {
                        // 如果没有指定重定向文件名，那么重定向到以jobid命名的缓存中
                        name = LogUtil.cacheDir() + File.separator + Constants.PID + File.separator + jobId;
                        cacheLocation = name;

                        // 限制同时重定向到文件的异步命令数量不超过8个
                        if (getRedirectJobCount() == 8) {
                            throw new IllegalStateException("The amount of async command that saving result to file can't > 8");
                        }
                    }
                    // 创建重定向处理器，>>表示追加，>表示覆盖
                    redirectHandler = new RedirectHandler(name, ">>".equals(tokenValue));
                    break;
                }
            }
            // 根据是否为管道命令，将token添加到不同的列表
            if (isPipeline) {
                pipelineTokens.add(remainingToken);
            } else {
                remaining.add(remainingToken);
            }
        }
        // 注入管道处理器
        injectHandler(stdoutHandlerChain, pipelineTokens);

        // 设置输出处理链
        if (redirectHandler != null) {
            // 如果有重定向处理器，添加到处理链
            stdoutHandlerChain.add(redirectHandler);
            term.write("redirect output file will be: " + redirectHandler.getFilePath()+"\n");
        } else {
            // 添加终端处理器，输出到终端
            stdoutHandlerChain.add(new TermHandler(term));
            // 如果全局配置要求保存结果，添加重定向处理器保存结果
            if (GlobalOptions.isSaveResult) {
                stdoutHandlerChain.add(new RedirectHandler());
            }
        }

        // 创建进程输出对象
        ProcessOutput processOutput = new ProcessOutput(stdoutHandlerChain, cacheLocation, term);
        // 创建进程实现对象
        ProcessImpl process = new ProcessImpl(command, remaining, command.processHandler(), processOutput, resultDistributor);
        process.setTty(term);
        return process;
    }

    /**
     * 获取重定向文件名
     *
     * 从tokens中提取重定向的目标文件名
     * 文件名是重定向符号（>或>>）后的第一个文本token
     *
     * @param tokens tokens迭代器
     * @return 文件名，如果没有找到则返回null
     */
    private String getRedirectFileName(ListIterator<CliToken> tokens) {
        while (tokens.hasNext()) {
            CliToken token = tokens.next();
            if (token.isText()) {
                // 找到第一个文本类型的token作为文件名
                return token.value();
            }
        }
        return null;
    }

    /**
     * 将管道处理器注入到输出处理链
     *
     * 根据管道命令的tokens创建对应的处理器，并添加到输出处理链中
     * 管道处理器会对前一个命令的输出进行处理
     *
     * @param stdoutHandlerChain 标准输出处理链
     * @param pipelineTokens 管道命令的tokens列表
     */
    private void injectHandler(List<Function<String, String>> stdoutHandlerChain, List<CliToken> pipelineTokens) {
        if (!pipelineTokens.isEmpty()) {
            // 根据管道tokens创建处理器
            StdoutHandler handler = StdoutHandler.inject(pipelineTokens);
            if (handler != null) {
                // 将处理器添加到输出处理链
                stdoutHandlerChain.add(handler);
            }
            // 清空管道tokens列表
            pipelineTokens.clear();
        }
    }

    /**
     * 关闭Job控制器（无参数版本）
     *
     * 终止所有正在运行的Job
     */
    @Override
    public void close() {
        close(null);
    }
}
