package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.TokenUtils;

import java.util.List;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class ShellLineHandler implements Handler<String> {

    private ShellImpl shell;
    private Term term;

    public ShellLineHandler(ShellImpl shell) {
        this.shell = shell;
        this.term = shell.term();
    }

    @Override
    public void handle(String line) {
        if (line == null) {
            // EOF
            handleExit();
            return;
        }

        List<CliToken> tokens = CliTokens.tokenize(line);
        CliToken first = TokenUtils.findFirstTextToken(tokens);
        if (first == null) {
            // For now do like this
            shell.readline();
            return;
        }

        String name = first.value();
        if (name.equals("exit") || name.equals("logout") || name.equals("q") || name.equals("quit")) {
            handleExit();
            return;
        } else if (name.equals("jobs")) {
            handleJobs();
            return;
        } else if (name.equals("fg")) {
            handleForeground(tokens);
            return;
        } else if (name.equals("bg")) {
            handleBackground(tokens);
            return;
        } else if (name.equals("kill")) {
            handleKill(tokens);
            return;
        }

        Job job = createJob(tokens);
        if (job != null) {
            job.run();
        }
    }

    private int getJobId(String arg) {
        int result = -1;
        try {
            if (arg.startsWith("%")) {
                result = Integer.parseInt(arg.substring(1));
            } else {
                result = Integer.parseInt(arg);
            }
        } catch (Exception e) {
        }
        return result;
    }

    private Job createJob(List<CliToken> tokens) {
        Job job;
        try {
            job = shell.createJob(tokens);
        } catch (Exception e) {
            term.echo(e.getMessage() + "\n");
            shell.readline();
            return null;
        }
        return job;
    }

    private void handleKill(List<CliToken> tokens) {
        String arg = TokenUtils.findSecondTokenText(tokens);
        if (arg == null) {
            term.write("kill: usage: kill job_id\n");
            shell.readline();
            return;
        }
        Job job = shell.jobController().getJob(getJobId(arg));
        if (job == null) {
            term.write(arg + " : no such job\n");
            shell.readline();
        } else {
            job.terminate();
            term.write("kill job " + job.id() + " success\n");
            shell.readline();
        }
    }

    private void handleBackground(List<CliToken> tokens) {
        String arg = TokenUtils.findSecondTokenText(tokens);
        Job job;
        if (arg == null) {
            job = shell.getForegroundJob();
        } else {
            job = shell.jobController().getJob(getJobId(arg));
        }
        if (job == null) {
            term.write(arg + " : no such job\n");
            shell.readline();
        } else {
            if (job.status() == ExecStatus.STOPPED) {
                job.resume(false);
                term.echo(shell.statusLine(job, ExecStatus.RUNNING));
                shell.readline();
            } else {
                term.write("job " + job.id() + " is already running\n");
                shell.readline();
            }
        }
    }

    private void handleForeground(List<CliToken> tokens) {
        String arg = TokenUtils.findSecondTokenText(tokens);
        Job job;
        if (arg == null) {
            job = shell.getForegroundJob();
        } else {
            job = shell.jobController().getJob(getJobId(arg));
        }
        if (job == null) {
            term.write(arg + " : no such job\n");
            shell.readline();
        } else {
            if (job.getSession() != shell.session()) {
                term.write("job " + job.id() + " doesn't belong to this session, so can not fg it\n");
                shell.readline();
            } else if (job.status() == ExecStatus.STOPPED) {
                job.resume(true);
            } else if (job.status() == ExecStatus.RUNNING) {
                // job is running
                job.toForeground();
            } else {
                term.write("job " + job.id() + " is already terminated, so can not fg it\n");
                shell.readline();
            }
        }
    }

    private void handleJobs() {
        for (Job job : shell.jobController().jobs()) {
            String statusLine = shell.statusLine(job, job.status());
            term.write(statusLine);
        }
        shell.readline();
    }

    private void handleExit() {
        term.close();
    }
}
