package com.taobao.arthas.core.shell;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobController;

import java.util.List;

/**
 * An interactive session between a consumer and a shell.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Shell {

    /**
     * Create a job, the created job should then be executed with the {@link Job#run()} method.
     *
     * @param line the command line creating this job
     * @return the created job
     */
    Job createJob(List<CliToken> line);

    /**
     * See {@link #createJob(List)}
     */
    Job createJob(String line);

    /**
     * @return the shell's job controller
     */
    JobController jobController();

    /**
     * @return the current shell session
     */
    Session session();

    /**
     * Close the shell.
     */
    void close(String reason);
}

