package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.session.Session;

import java.util.List;

/**
 * 自动补全接口
 *
 * 该接口定义了命令行自动补全的相关操作，包括获取会话信息、原始命令行、
 * 解析后的命令行Token以及完成补全的方法。
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Completion {

    /**
     * 获取Shell当前会话
     *
     * 会话对象可以访问各种数据，例如用于文件补全的当前路径等
     *
     * @return Shell当前会话对象
     */
    Session session();

    /**
     * 获取正在补全的当前行的原始格式
     *
     * 原始格式表示未进行任何字符转义处理的命令行字符串
     *
     * @return 原始命令行字符串
     */
    String rawLine();

    /**
     * 获取正在补全的当前行，以预解析的Token列表形式返回
     *
     * Token列表是对命令行进行解析后得到的，每个Token代表命令行中的一个元素
     *
     * @return 预解析的命令行Token列表
     */
    List<CliToken> lineTokens();

    /**
     * 结束补全，提供候选列表
     *
     * 这些候选列表将由Shell在控制台上显示出来供用户选择
     *
     * @param candidates 候选补全列表
     */
    void complete(List<String> candidates);

    /**
     * 结束补全，提供一个将插入以完成命令行的值
     *
     * @param value 用于补全的值
     * @param terminal 如果值为true，表示该值是终结性的，可以进一步补全；否则表示补全已完成
     */
    void complete(String value, boolean terminal);

}
