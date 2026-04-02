package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.TokenUtils;
import com.taobao.arthas.core.view.Ansi;

import java.util.List;

/**
 * Shell行处理器
 * <p>
 * 这是Shell的核心处理器之一，负责处理用户输入的每一行命令。
 * 它实现了类似Unix Shell的功能，包括：
 * <ul>
 * <li>处理内置命令：exit、logout、q、quit、jobs、fg、bg、kill</li>
 * <li>创建和执行用户任务</li>
 * <li>管理前台和后台任务</li>
 * <li>处理任务的生命周期（停止、恢复、终止）</li>
 * </ul>
 * </p>
 *
 * @author beiwei30 on 23/11/2016.
 */
public class ShellLineHandler implements Handler<String> {

    /**
     * Shell实现对象
     * 提供Shell的核心功能，包括任务管理、会话管理等
     */
    private ShellImpl shell;

    /**
     * 终端对象
     * 用于与用户交互，包括读取输入和输出信息
     */
    private Term term;

    /**
     * 构造函数
     *
     * @param shell Shell实现对象，用于访问Shell功能
     */
    public ShellLineHandler(ShellImpl shell) {
        this.shell = shell;
        this.term = shell.term();
    }

    /**
     * 处理用户输入的命令行
     * <p>
     * 这是核心方法，处理用户输入的每一行文本。处理流程：
     * <ol>
     * <li>检查是否为EOF（文件结束符），如果是则调用handleExit()</li>
     * <li>将输入行解析为命令令牌（tokens）</li>
     * <li>根据第一个令牌判断命令类型：</li>
     *   <ul>
     *   <li>exit/logout/q/quit：退出当前会话</li>
     *   <li>jobs：列出所有任务</li>
     *   <li>fg：将任务切换到前台</li>
     *   <li>bg：将任务切换到后台</li>
     *   <li>kill：终止指定任务</li>
     *   </ul>
     * <li>如果不是内置命令，则创建并执行一个新任务</li>
     * </ol>
     * </p>
     *
     * @param line 用户输入的命令行文本
     */
    @Override
    public void handle(String line) {
        // 检查是否为EOF（文件结束符），通常在用户按Ctrl+D时出现
        if (line == null) {
            // EOF情况，处理退出
            handleExit();
            return;
        }

        // 将输入行解析为命令令牌列表
        List<CliToken> tokens = CliTokens.tokenize(line);

        // 查找第一个文本类型的令牌（通常是命令名称）
        CliToken first = TokenUtils.findFirstTextToken(tokens);
        if (first == null) {
            // 如果没有找到有效的令牌，直接重新读取下一行
            // For now do like this
            shell.readline();
            return;
        }

        // 获取命令名称
        String name = first.value();

        // 处理各种退出命令
        if (name.equals("exit") || name.equals("logout") || name.equals("q") || name.equals("quit")) {
            handleExit();
            return;
        } else if (name.equals("jobs")) {
            // 处理jobs命令，列出所有任务
            handleJobs();
            return;
        } else if (name.equals("fg")) {
            // 处理fg命令，将任务切换到前台
            handleForeground(tokens);
            return;
        } else if (name.equals("bg")) {
            // 处理bg命令，将任务切换到后台
            handleBackground(tokens);
            return;
        } else if (name.equals("kill")) {
            // 处理kill命令，终止指定任务
            handleKill(tokens);
            return;
        }

        // 如果不是内置命令，则创建一个新的任务
        Job job = createJob(tokens);
        if (job != null) {
            // 运行新创建的任务
            job.run();
        }
    }

    /**
     * 从参数字符串中提取任务ID
     * <p>
     * 任务ID可以是以下格式：
     * <ul>
     * <li>%数字：如%1，表示任务号为1的任务</li>
     * <li>纯数字：如1，也表示任务号为1的任务</li>
     * </ul>
     * </p>
     *
     * @param arg 包含任务ID的参数字符串
     * @return 解析出的任务ID，如果解析失败则返回-1
     */
    private int getJobId(String arg) {
        int result = -1;
        try {
            // 检查参数是否以%开头
            if (arg.startsWith("%")) {
                // 去掉%前缀后解析数字
                result = Integer.parseInt(arg.substring(1));
            } else {
                // 直接解析数字
                result = Integer.parseInt(arg);
            }
        } catch (Exception e) {
            // 解析失败，保持-1的返回值
        }
        return result;
    }

    /**
     * 创建一个新任务
     * <p>
     * 根据命令令牌列表创建一个任务对象。
     * 如果创建过程中出现异常，会向终端输出错误信息并重新读取输入。
     * </p>
     *
     * @param tokens 命令令牌列表，包含命令名称和参数
     * @return 创建的任务对象，如果创建失败则返回null
     */
    private Job createJob(List<CliToken> tokens) {
        Job job;
        try {
            // 调用shell的createJob方法创建任务
            job = shell.createJob(tokens);
        } catch (Exception e) {
            // 创建失败，输出错误信息
            term.echo(e.getMessage() + "\n");
            // 重新读取下一行输入
            shell.readline();
            return null;
        }
        return job;
    }

    /**
     * 处理kill命令
     * <p>
     * 终止指定的任务。用法：kill job_id
     * </p>
     * <p>
     * 处理流程：
     * <ol>
     * <li>获取第二个令牌作为参数</li>
     * <li>如果参数为空，显示用法提示</li>
     * <li>根据参数查找对应的任务</li>
     * <li>如果任务不存在，显示错误信息</li>
     * <li>如果任务存在，调用terminate()终止任务</li>
     * </ol>
     * </p>
     *
     * @param tokens 命令令牌列表
     */
    private void handleKill(List<CliToken> tokens) {
        // 获取第二个令牌的文本内容作为参数（任务ID）
        String arg = TokenUtils.findSecondTokenText(tokens);
        if (arg == null) {
            // 参数为空，显示用法提示
            term.write("kill: usage: kill job_id\n");
            shell.readline();
            return;
        }

        // 从任务控制器中获取指定ID的任务
        Job job = shell.jobController().getJob(getJobId(arg));
        if (job == null) {
            // 任务不存在，显示错误信息
            term.write(arg + " : no such job\n");
            shell.readline();
        } else {
            // 终止任务
            job.terminate();
            // 显示成功信息
            term.write("kill job " + job.id() + " success\n");
            // 重新读取下一行输入
            shell.readline();
        }
    }

    /**
     * 处理bg命令
     * <p>
     * 将任务切换到后台运行。用法：bg [job_id]
     * </p>
     * <p>
     * 处理流程：
     * <ol>
     * <li>获取参数（可选），如果未提供参数则使用当前前台任务</li>
     * <li>查找指定的任务</li>
     * <li>如果任务不存在，显示错误信息</li>
     * <li>如果任务存在且处于停止状态，恢复其运行</li>
     * <li>如果任务已经在运行，显示提示信息</li>
     * </ol>
     * </p>
     *
     * @param tokens 命令令牌列表
     */
    private void handleBackground(List<CliToken> tokens) {
        // 获取第二个令牌的文本内容作为参数
        String arg = TokenUtils.findSecondTokenText(tokens);
        Job job;

        if (arg == null) {
            // 参数为空，获取当前前台任务
            job = shell.getForegroundJob();
        } else {
            // 根据参数查找指定任务
            job = shell.jobController().getJob(getJobId(arg));
        }

        if (job == null) {
            // 任务不存在，显示错误信息
            term.write(arg + " : no such job\n");
            shell.readline();
        } else {
            // 检查任务状态
            if (job.status() == ExecStatus.STOPPED) {
                // 任务处于停止状态，恢复运行（false表示后台运行）
                job.resume(false);
                // 显示任务状态变更信息
                term.echo(shell.statusLine(job, ExecStatus.RUNNING));
                // 重新读取下一行输入
                shell.readline();
            } else {
                // 任务已经在运行，显示提示信息
                term.write("job " + job.id() + " is already running\n");
                shell.readline();
            }
        }
    }

    /**
     * 处理fg命令
     * <p>
     * 将任务切换到前台运行。用法：fg [job_id]
     * </p>
     * <p>
     * 处理流程：
     * <ol>
     * <li>获取参数（可选），如果未提供参数则使用当前前台任务</li>
     * <li>查找指定的任务</li>
     * <li>如果任务不存在，显示错误信息</li>
     * <li>检查任务是否属于当前会话</li>
     * <li>根据任务状态执行相应操作：
     *   <ul>
     *   <li>停止状态：恢复运行并切换到前台</li>
     *   <li>运行状态：直接切换到前台</li>
     *   <li>已终止：显示错误信息</li>
     *   </ul>
     * </li>
     * </ol>
     * </p>
     *
     * @param tokens 命令令牌列表
     */
    private void handleForeground(List<CliToken> tokens) {
        // 获取第二个令牌的文本内容作为参数
        String arg = TokenUtils.findSecondTokenText(tokens);
        Job job;

        if (arg == null) {
            // 参数为空，获取当前前台任务
            job = shell.getForegroundJob();
        } else {
            // 根据参数查找指定任务
            job = shell.jobController().getJob(getJobId(arg));
        }

        if (job == null) {
            // 任务不存在，显示错误信息
            term.write(arg + " : no such job\n");
            shell.readline();
        } else {
            // 检查任务是否属于当前会话
            if (job.getSession() != shell.session()) {
                // 任务不属于当前会话，无法切换到前台
                term.write("job " + job.id() + " doesn't belong to this session, so can not fg it\n");
                shell.readline();
            } else if (job.status() == ExecStatus.STOPPED) {
                // 任务处于停止状态，恢复运行并切换到前台（true表示前台运行）
                job.resume(true);
            } else if (job.status() == ExecStatus.RUNNING) {
                // 任务正在运行，将其切换到前台
                // job is running
                job.toForeground();
            } else {
                // 任务已终止，无法切换到前台
                term.write("job " + job.id() + " is already terminated, so can not fg it\n");
                shell.readline();
            }
        }
    }

    /**
     * 处理jobs命令
     * <p>
     * 列出所有当前任务及其状态。
     * </p>
     * <p>
     * 遍历任务控制器中的所有任务，为每个任务生成状态行并输出到终端。
     * </p>
     */
    private void handleJobs() {
        // 遍历所有任务
        for (Job job : shell.jobController().jobs()) {
            // 为每个任务生成状态行
            String statusLine = shell.statusLine(job, job.status());
            // 将状态行写入终端
            term.write(statusLine);
        }
        // 重新读取下一行输入
        shell.readline();
    }

    /**
     * 处理退出命令
     * <p>
     * 当用户输入exit、logout、q或quit命令时调用此方法。
     * </p>
     * <p>
     * 执行以下操作：
     * <ol>
     * <li>生成绿色的退出提示信息</li>
     * <li>提示用户会话已终止但Arthas仍在后台运行</li>
     * <li>提示用户使用stop命令完全关闭Arthas</li>
     * <li>关闭终端</li>
     * </ol>
     * </p>
     */
    private void handleExit() {
        // 构建绿色的提示信息
        String msg = Ansi.ansi().fg(Ansi.Color.GREEN).a("Session has been terminated.\n"
                + "Arthas is still running in the background.\n"
                + "To completely shutdown arthas, please execute the 'stop' command.\n").reset().toString();
        // 输出提示信息
        term.write(msg);
        // 关闭终端
        term.close();
    }
}
