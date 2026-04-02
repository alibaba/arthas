package com.taobao.arthas.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 命令行执行工具类
 * 用于在本地命令行执行命令并返回执行结果
 *
 * @author alessandro[at]perucchi[dot]org
 */
public class ExecutingCommand {

    /**
     * 私有构造函数
     * 防止实例化，该类为工具类，只提供静态方法
     */
    private ExecutingCommand() {
    }

    /**
     * 在本地命令行执行命令并返回结果
     * 该方法会将字符串命令按空格分割后执行
     *
     * @param cmdToRun 要执行的命令字符串
     * @return 命令执行结果的字符串列表，如果命令执行失败则返回空列表
     */
    public static List<String> runNative(String cmdToRun) {
        // 将命令字符串按空格分割成命令数组
        String[] cmd = cmdToRun.split(" ");
        return runNative(cmd);
    }

    /**
     * 在本地命令行执行命令并逐行返回结果
     *
     * @param cmdToRunWithArgs 要执行的命令及其参数数组
     * @return 命令执行结果的字符串列表（每行作为一个元素），如果命令执行失败则返回空列表
     */
    public static List<String> runNative(String[] cmdToRunWithArgs) {
        Process p = null;
        try {
            // 使用Runtime执行本地命令
            p = Runtime.getRuntime().exec(cmdToRunWithArgs);
        } catch (SecurityException e) {
            // 安全异常：没有权限执行命令
            AnsiLog.trace("Couldn't run command {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(e);
            return new ArrayList<String>(0);
        } catch (IOException e) {
            // IO异常：命令执行失败
            AnsiLog.trace("Couldn't run command {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(e);
            return new ArrayList<String>(0);
        }

        // 创建列表用于存储命令输出的每一行
        ArrayList<String> sa = new ArrayList<String>();
        // 创建缓冲读取器读取进程的输入流
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String line;
            // 逐行读取命令输出
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            // 等待进程执行完成
            p.waitFor();
        } catch (IOException e) {
            // IO异常：读取命令输出失败
            AnsiLog.trace("Problem reading output from {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(e);
            return new ArrayList<String>(0);
        } catch (InterruptedException ie) {
            // 中断异常：等待进程完成时被中断
            AnsiLog.trace("Problem reading output from {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(ie);
            // 恢复中断状态
            Thread.currentThread().interrupt();
        } finally {
            // 关闭读取器，释放资源
            IOUtils.close(reader);
        }
        return sa;
    }

    /**
     * 返回指定命令执行结果的第一行
     *
     * @param cmd2launch 要执行的命令字符串
     * @return 命令执行结果的第一行字符串，如果命令执行失败则返回空字符串
     */
    public static String getFirstAnswer(String cmd2launch) {
        // 获取第0行的结果
        return getAnswerAt(cmd2launch, 0);
    }

    /**
     * 返回指定命令执行结果中指定行（从0开始）的内容
     *
     * @param cmd2launch 要执行的命令字符串
     * @param answerIdx 要获取的结果行索引（从0开始）
     * @return 指定行的完整内容，如果索引无效或命令执行失败则返回空字符串
     */
    public static String getAnswerAt(String cmd2launch, int answerIdx) {
        // 执行命令并获取结果列表
        List<String> sa = ExecutingCommand.runNative(cmd2launch);

        // 检查索引是否有效
        if (answerIdx >= 0 && answerIdx < sa.size()) {
            return sa.get(answerIdx);
        }
        return "";
    }

}
