package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2019/4/24
 *
 * @author xuzhiyi
 */
public abstract class TimerCommand extends AnnotatedCommand {

    private volatile Timer timer;

    private AtomicInteger count = new AtomicInteger();

    abstract long getInterval();

    abstract int getNumOfExecutions();

    abstract String getPrefix();

    public void schedule(final CommandProcess process) {
        Session session = process.session();
        timer = new Timer(getPrefix() + session.getSessionId(), true);

        // ctrl-C support
        process.interruptHandler(new TimerInterruptHandler(process, timer));

        /*
         * 通过handle回调，在suspend和end时停止timer，resume时重启timer
         */
        Handler<Void> stopHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                stop();
            }
        };

        Handler<Void> restartHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                restart(process);
            }
        };
        process.suspendHandler(stopHandler);
        process.resumeHandler(restartHandler);
        process.endHandler(stopHandler);

        // q exit support
        process.stdinHandler(new QExitHandler(process));

        // start the timer
        if (getInterval() > 0) {
            timer.scheduleAtFixedRate(new InnerTimerTask(process), 0, getInterval());
        } else {
            timer.schedule(new InnerTimerTask(process), 0);
        }
    }

    synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    synchronized void restart(CommandProcess process) {
        if (timer == null) {
            Session session = process.session();
            timer = new Timer(getPrefix() + session.getSessionId(), true);
            timer.scheduleAtFixedRate(new InnerTimerTask(process), 0, getInterval());
        }
    }

    abstract void run(CommandProcess process);

    private class InnerTimerTask extends TimerTask {
        private CommandProcess process;

        public InnerTimerTask(CommandProcess process) {
            this.process = process;
        }

        @Override
        public void run() {
            if (count.get() >= getNumOfExecutions()) {
                // stop the timer
                timer.cancel();
                timer.purge();
                process.write("Process ends after " + getNumOfExecutions() + " time(s).\n");
                process.end();
                return;
            }

            TimerCommand.this.run(process);

            count.incrementAndGet();
            process.times().incrementAndGet();

            if (getInterval() <= 0) {
                stop();
                process.end();
            }
        }
    }
}
