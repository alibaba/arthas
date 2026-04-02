package com.taobao.arthas.core.shell.history.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.history.HistoryManager;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令历史管理器实现类
 *
 * 该类实现了 HistoryManager 接口，负责管理 Arthas 命令行工具的命令历史记录。
 * 主要功能包括：
 * 1. 将命令历史保存到文件
 * 2. 从文件加载命令历史
 * 3. 添加新的命令到历史记录
 * 4. 获取和清除命令历史
 * 5. 限制内存中保存的历史记录数量（最多 500 条）
 *
 * @see io.termd.core.readline.Readline#history
 * @author gongdewei 2020/4/8
 */
public class HistoryManagerImpl implements HistoryManager {

    /**
     * 内存中保存的历史记录最大数量
     * 当历史记录超过此数量时，会自动删除最旧的记录
     */
    private static final int MAX_HISTORY_SIZE = 500;

    /**
     * 日志记录器，用于记录操作日志和错误信息
     */
    private static final Logger logger = LoggerFactory.getLogger(HistoryManagerImpl.class);

    /**
     * 命令历史记录列表
     * 使用 ArrayList 存储命令字符串，按时间顺序排列
     */
    private List<String> history = new ArrayList<String>();

    /**
     * 默认构造函数
     *
     * 创建一个空的命令历史管理器实例
     */
    public HistoryManagerImpl() {
    }

    /**
     * 保存命令历史到文件
     *
     * 该方法将当前内存中的所有命令历史记录保存到文件中。
     * 文件路径由 Constants.CMD_HISTORY_FILE 指定。
     *
     * 注意：该方法使用 synchronized 修饰，确保线程安全
     */
    @Override
    public synchronized void saveHistory() {
        try {
            // 调用 FileUtils 工具类将历史记录列表保存到文件
            // 文件路径由常量 CMD_HISTORY_FILE 指定
            FileUtils.saveCommandHistoryString(history, new File(Constants.CMD_HISTORY_FILE));
        } catch (Throwable e) {
            // 如果保存过程中发生任何异常，记录错误日志
            // 使用 Throwable 捕获所有可能的错误，包括 Error 和 Exception
            logger.error("save command history failed", e);
        }
    }

    /**
     * 从文件加载命令历史
     *
     * 该方法从文件中读取命令历史记录，并替换当前内存中的历史记录。
     * 文件路径由 Constants.CMD_HISTORY_FILE 指定。
     *
     * 注意：该方法使用 synchronized 修饰，确保线程安全
     */
    @Override
    public synchronized void loadHistory() {
        try {
            // 调用 FileUtils 工具类从文件加载历史记录列表
            // 加载的历史记录会替换当前的 history 列表
            history = FileUtils.loadCommandHistoryString(new File(Constants.CMD_HISTORY_FILE));
        } catch (Throwable e) {
            // 如果加载过程中发生任何异常，记录错误日志
            // 使用 Throwable 捕获所有可能的错误，包括 Error 和 Exception
            logger.error("load command history failed", e);
        }
    }

    /**
     * 清空命令历史
     *
     * 该方法会清除内存中的所有命令历史记录。
     * 注意：此操作不会删除历史文件，只是清空内存中的记录。
     *
     * 注意：该方法使用 synchronized 修饰，确保线程安全
     */
    @Override
    public synchronized void clearHistory() {
        // 清空历史记录列表
        this.history.clear();
    }

    /**
     * 添加命令到历史记录
     *
     * 该方法将一条新的命令添加到历史记录的末尾。
     * 如果当前历史记录数量已达到最大值（MAX_HISTORY_SIZE），
     * 则会自动删除最旧的记录（列表头部的记录）以腾出空间。
     *
     * @param commandLine 要添加的命令行字符串
     *
     * 注意：该方法使用 synchronized 修饰，确保线程安全
     */
    @Override
    public synchronized void addHistory(String commandLine) {
        // 检查历史记录数量是否已达到最大值
        // 如果达到或超过最大值，则删除最旧的记录
        while (history.size() >= MAX_HISTORY_SIZE) {
            // 删除列表头部的记录（最旧的记录）
            history.remove(0);
        }

        // 将新命令添加到历史记录列表的末尾
        history.add(commandLine);
    }

    /**
     * 获取命令历史记录
     *
     * 该方法返回当前所有命令历史记录的副本。
     * 返回的是一个新的 ArrayList，因此对返回列表的修改不会影响原始历史记录。
     *
     * @return 命令历史记录列表的副本，按时间顺序排列
     *
     * 注意：该方法使用 synchronized 修饰，确保线程安全
     */
    @Override
    public synchronized List<String> getHistory() {
        // 创建并返回历史记录列表的副本
        // 使用 new ArrayList(history) 创建新列表，避免返回内部可变列表
        return new ArrayList<String>(history);
    }

    /**
     * 设置命令历史记录
     *
     * 该方法使用提供的历史记录列表替换当前的历史记录。
     * 通常用于批量导入历史记录或恢复历史记录。
     *
     * @param history 要设置的历史记录列表，将完全替换当前的历史记录
     *
     * 注意：该方法使用 synchronized 修饰，确保线程安全
     */
    @Override
    public synchronized void setHistory(List<String> history) {
        // 直接替换历史记录列表的引用
        this.history = history;
    }

}
