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

import io.netty.util.internal.StringUtil;

/**
 * <pre>
 * 此gc命令提供的后端一次性获取，然后展示；
 * 后期再改造一下
 * </pre>
 * 
 * @author wangdong 2020年06月01日 下午2:06:21
 */
@Name("gc")
@Summary("Display gc info")
@Description(Constants.EXAMPLE + "  gc\n" + "  gc -i 1000  \n" + "  gc -i 1000 -n 5\n")
public class GcCommand extends AnnotatedCommand {

	private static final Logger logger = LoggerFactory.getLogger(GcCommand.class);

	private volatile int interval = -1;// 默认间隔时间

	private volatile int loopCount = -1;// 循环次数

	private volatile int priCount = 0;// 初始化次数

	private long pid = 0;

	private volatile Timer timer;

	@Option(shortName = "n", longName = "gc-show-count")
	@Description("the number of gc info to show, the default count is 5.")
	public void setLoopCount(Integer count) {
		this.loopCount = count;
	}

	@Option(shortName = "i", longName = "gc-show-intervalTime")
	@Description("get gc info interval,the default interval is 1000ms.")
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

			timer = new Timer("Timer-for-arthas-gc-" + process.session().getSessionId(), true);

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
			if (priCount > loopCount) {
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
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (priCount == 0) {
					content.append(line);
					content.append("\n");
				} else {
					if (!line.contains("S0")) { // 命令每次返回结果，都会有列名，只取第一次执行的列名，后续的直接取值
						content.append(line);
						content.append("\n");
					}
				}

			}
			StringBuilder errorMessage = new StringBuilder();
			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = error.readLine()) != null) {
				errorMessage.append(line).append("\n");
			}
			if (errorMessage.toString().length()!=0) {
				logger.error("jstat -gcutil command has error:"+errorMessage.toString());
				content.append(errorMessage);
			}
			process.write(content.toString());
		} catch (IOException e) {
			logger.error("jstat -gcutil command has error!", e);
			process.write("jstat -gcutil has error!\n");
		} finally {
			if (p != null) {
				p.destroy();
			}
			priCount++;
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
