package com.taobao.arthas.core.command.basic1000;


import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.*;

/**
 * Tee命令类
 *
 * 该命令类似于Linux中的tee命令，用于在管道中将输出结果同时写入文件和传递给下一个命令。
 * 该命令只能在管道中使用，不能单独执行。
 *
 * 使用场景：
 * - 将命令输出保存到文件的同时，继续在管道中处理
 * - 支持覆盖模式（默认）和追加模式（-a参数）
 *
 * 使用示例：
 * - sysprop | tee /path/to/logfile | grep java: 将sysprop输出写入文件，然后过滤java相关属性
 * - sysprop | tee -a /path/to/logfile | grep java: 使用追加模式写入文件
 *
 * @author min.yang
 */
@Name("tee")
@Summary("tee command for pipes." )
@Description(Constants.EXAMPLE +
        " sysprop | tee /path/to/logfile | grep java \n" +
        " sysprop | tee -a /path/to/logfile | grep java \n"
        + Constants.WIKI + Constants.WIKI_HOME + "tee")
public class TeeCommand extends AnnotatedCommand {

    /** 输出文件的路径 */
    private String filePath;
    /** 是否使用追加模式，true表示追加，false表示覆盖 */
    private boolean append;

    /**
     * 设置输出文件路径
     *
     * @param filePath 文件的绝对路径或相对路径
     */
    @Argument(index = 0, argName = "file", required = false)
    @Description("File path")
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 设置是否使用追加模式
     *
     * @param append true表示追加到文件末尾，false表示覆盖原有文件内容
     */
    @Option(shortName = "a", longName = "append", flag = true)
    @Description("Append to file")
    public void setRegEx(boolean append) {
        this.append = append;
    }

    /**
     * 处理命令执行逻辑
     *
     * 注意：tee命令只能在管道中使用，如果直接执行会返回错误信息。
     * 实际的管道处理逻辑在其他地方实现，该方法只是阻止直接调用。
     *
     * @param process 命令处理进程对象
     */
    @Override
    public void process(CommandProcess process) {
        // tee命令只能在管道中使用，直接执行则返回错误
        process.end(-1, "The tee command only for pipes. See 'tee --help'");
    }

    /**
     * 获取输出文件路径
     *
     * @return 文件路径字符串
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 判断是否使用追加模式
     *
     * @return true表示追加模式，false表示覆盖模式
     */
    public boolean isAppend() {
        return append;
    }
}
