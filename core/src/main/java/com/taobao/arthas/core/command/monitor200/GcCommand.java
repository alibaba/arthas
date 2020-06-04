package com.taobao.arthas.core.command.monitor200;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.affect.Affect;
import com.taobao.arthas.core.util.affect.RowAffect;
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
 *  @author wangdong 2020年06月01日 下午2:06:21
 */
@Name("gc")
@Summary("Display gc info")
@Description(Constants.EXAMPLE + "  gc\n" + "  gc -i 1000  -1\n" + "  gc -i 1000 -n 5\n")
public class GcCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(GcCommand.class);

	private int interval = -1;// 获取gc执行情况间隔时间

	private int count = -1;// 获取gc执行情况次数

	private long pid = 0;

	@Option(shortName = "n", longName = "gc-show-count")
	@Description("The number of gc to show.")
	public void setCount(Integer count) {
		this.count = count;
	}

	@Option(shortName = "i", longName = "gc-show-intervalTime")
	@Description("Get gc info interval.")
	public void setInterval(int interval) {
		this.interval = interval;
	}

	@Override
	public void process(CommandProcess process) {
		Affect affect = new RowAffect();
		try {
			pid = process.session().getPid();
			if (pid > 0) {
				count = count==-1?5:count;
				interval = interval==-1?1000:interval;
				logger.info("pid:{},count:{},interval:{}",pid,count,interval);
				processGC(process);
			}else {
				process.write("pid cannot get!");
			}
		} finally {
			process.write(affect + "\n");
			process.end();
		}
	}


	private void processGC(CommandProcess process) {
		StringBuilder command = new StringBuilder("jstat -gcutil ");
		command.append(pid);
		command.append(" ");
		command.append(interval);
		command.append(" ");
		command.append(count);
		logger.info("command:{}",command.toString());
		StringBuilder content = new StringBuilder();
		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(command.toString());
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				content.append(line);
				content.append("\n");
			}
			p.destroy();
		} catch (IOException e) {
			process.write("jstat gc has error!\n");
		}
		process.write(content.toString());
	}
	
}
