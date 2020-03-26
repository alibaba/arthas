package com.taobao.arthas.core.shell.system;

import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.term.Term;

import java.util.List;
import java.util.Set;

/**
 * The job controller.<p/>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface JobController {

    /**
     * @return the active jobs
     */
    Set<Job> jobs();

    /**
     * Returns an active job in this session by its {@literal id}.
     *
     * @param id the job id
     * @return the job of {@literal null} when not found
     */
    Job getJob(int id);

    /**
     * Create a job wrapping a process.
     *
     * @param commandManager command manager
     * @param tokens    the command tokens
     * @param session     the current session
     * @param jobHandler  job event handler
     * @param term     telnet term
     * @param resultDistributor
     * @return the created job
     */
    Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, Session session, JobListener jobHandler, Term term, ResultDistributor resultDistributor);

    /**
     * Close the controller and terminate all the underlying jobs, a closed controller does not accept anymore jobs.
     */
    void close(Handler<Void> completionHandler);

    /**
     * Close the shell session and terminate all the underlying jobs.
     */
    void close();

}
