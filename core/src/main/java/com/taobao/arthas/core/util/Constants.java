package com.taobao.arthas.core.util;

import java.io.File;

import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.view.Ansi;

/**
 * 常量定义类
 * <p>
 * 定义Arthas项目中使用的各种常量，包括提示信息、命令提示符、配置文件路径等
 * </p>
 *
 * @author ralf0131 2016-12-28 16:20.
 */
public class Constants {

    /**
     * 私有构造函数，防止实例化
     * <p>
     * 这是一个工具类，只包含静态常量，不应该被实例化
     * </p>
     */
    private Constants() {
    }

    /**
     * 中断提示消息
     * <p>
     * 提示用户可以通过按Q键或Ctrl+C来中断当前操作
     * </p>
     */
    public static final String Q_OR_CTRL_C_ABORT_MSG = "Press Q or Ctrl+C to abort.";

    /**
     * 空字符串常量
     * <p>
     * 用于表示空字符串，避免重复创建空字符串对象
     * </p>
     */
    public static final String EMPTY_STRING = "";

    /**
     * 默认命令提示符
     * <p>
     * Arthas命令行默认的提示符，显示为"$ "
     * </p>
     */
    public static final String DEFAULT_PROMPT = "$ ";

    /**
     * 带颜色的命令提示符
     * <p>
     * 使用黄色ANSI颜色代码的命令提示符，提升用户体验
     * 原始字符串格式："[33m$ [m"
     * </p>
     */
    public static final String COLOR_PROMPT = Ansi.ansi().fg(Ansi.Color.YELLOW).a(DEFAULT_PROMPT).reset().toString();

    /**
     * 方法执行耗时变量名
     * <p>
     * 用于存储方法执行耗时的变量名称，在命令执行结果中显示
     * </p>
     */
    public static final String COST_VARIABLE = "cost";

    /**
     * 命令历史文件路径
     * <p>
     * 存储用户在Arthas中执行过的命令历史记录，路径为用户主目录下的.arthas/history文件
     * </p>
     */
    public static final String CMD_HISTORY_FILE = System.getProperty("user.home") + File.separator + ".arthas" + File.separator + "history";

    /**
     * 当前进程的PID（进程ID）
     * <p>
     * 通过PidUtils获取当前Java进程的进程ID，用于标识当前被监控的进程
     * </p>
     */
    public static final String PID = PidUtils.currentPid();

}
