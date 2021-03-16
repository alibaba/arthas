package com.taobao.arthas.core.command.monitor200;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * <pre>
 * 此gc命令提供的后端一次性获取，然后展示；
 * 后期再改造一下
 * </pre>
 * 
 * @author wangdong 2020年06月01日 下午2:06:21
 */
@Name("gcinfo")
@Summary("Display gc info")
@Description(Constants.EXAMPLE + "  gcinfo \n" + "  gcinfo -i 1000  \n" + "  gcinfo -i 1000 -n 5\n")
public class GcinfoCommand extends AnnotatedCommand {

	private static final Logger logger = LoggerFactory.getLogger(GcinfoCommand.class);

	private volatile int interval = -1;// 默认间隔时间

	private volatile int loopCount = -1;// 循环次数

	private volatile int initCount = 0;// 初始化次数

	private long pid = 0;

	private volatile Timer timer;

	@Option(shortName = "n", longName = "gcinfo-show-count")
	@Description("the number of gc info to show, the default count is 5.")
	public void setLoopCount(Integer count) {
		this.loopCount = count;
	}

	@Option(shortName = "i", longName = "intervalTime")
	@Description("The intervalTime (in ms) between two executions, default is 1000 ms.")
	public void setInterval(int interval) {
		this.interval = interval;
	}

	@Override
	public void process(CommandProcess process) {
		pid = process.session().getPid();
		logger.info("pid:" + pid + ",internal:" + interval + ",n:" + loopCount);
		if (pid > 0) {
			loopCount = loopCount == -1 ? 5 : loopCount;
			interval = interval == -1 ? 1000 : interval;

			timer = new Timer("Timer-for-arthas-gcinfo-" + process.session().getSessionId(), true);

			// ctrl-C exit support
			process.interruptHandler(new GcInfoInterruptHandler(process, timer));

			// q exit support
			process.stdinHandler(new QExitHandler(process));
			// start the timer
			timer.scheduleAtFixedRate(new GCTimerTask(process), 0, interval);

		} else {
			process.write("get pid failure!");
		}

	}

	private class GCTimerTask extends TimerTask {

		private CommandProcess process;

		/**
		 * @param process
		 */
		public GCTimerTask(CommandProcess process) {
			this.process = process;
		}

		@Override
		public void run() {
			if (initCount > loopCount) {
				// stop the timer
				timer.cancel();
				timer.purge();
				process.write("Process ends after " + loopCount + " time(s).\n");
				process.end();
				return;
			}
			processGC(process);
		}

	}

	private void processGC(CommandProcess process) {
		StringBuilder content = new StringBuilder("");
		Runtime run = Runtime.getRuntime();
		Process p = null;
		try {
			logger.info("command:" + assembleCommand());
			p = run.exec(assembleCommand());
			getResponse(content, p);
			process.write(content.toString());
		} catch (IOException e) {
			logger.error("jstat -gcutil command has error!", e);
			process.write("jstat -gcutil has error!\n");
		} finally {
			if (p != null) {
				p.destroy();
			}
			initCount++;
		}

	}

	private void getResponse(StringBuilder content, Process p) {
		Thread redirectStdout = new Thread(new Runnable() {
		    @Override
		    public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    	try {
					String line = null;
					while ((line = br.readLine()) != null) {
						if (initCount == 0) {
							content.append(line);
							content.append("\n");
						} else {
							if (!line.contains("S0")) { // 命令每次返回结果，都会有列名，只取第一次执行的列名，后续的直接取值
								content.append(line);
								content.append("\n");
							}
						}

					} 
				} catch (Exception e) {
					logger.error("GcinfoCommand-redirectStdout has error:"+e.getMessage(),e);
					try {
						if (br!=null) {
							br.close();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
		    }
		});
		StringBuilder errorMessage = new StringBuilder();
		Thread redirectStderr = new Thread(new Runnable() {
		    @Override
		    public void run() {
				BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    	try {
					String line = null;
					while ((line = error.readLine()) != null) {
						errorMessage.append(line).append("\n");
					} 
				} catch (Exception e) {
					logger.error("GcinfoCommand-redirectStderr has error:"+e.getMessage(),e);
					try {
						if (error!=null) {
							error.close();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

		    }
		});
		redirectStdout.start();
		redirectStderr.start();
		try {
			redirectStdout.join();
			redirectStderr.join();
		} catch (InterruptedException e) {
			logger.error("redirectStdout or redirectStderr is interrupted,the error is:"+e.getMessage(),e);
		}

		if (errorMessage.toString().length() != 0) {
			logger.error("jstat -gcutil command has error:" + errorMessage.toString());
			content.append(errorMessage);
		}
	}

	private String assembleCommand() {
		StringBuilder command = new StringBuilder("jstat -gcutil ");
		command.append(pid);
		command.append(" ");
		command.append(interval);
		command.append(" ");
		command.append(1);
		return command.toString();
	}
}
