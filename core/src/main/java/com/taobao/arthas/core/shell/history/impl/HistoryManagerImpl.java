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
 * @see io.termd.core.readline.Readline#history
 * @author gongdewei 2020/4/8
 */
public class HistoryManagerImpl implements HistoryManager {
    /**
     * The max number of history item that will be saved in memory.
     */
    private static final int MAX_HISTORY_SIZE = 500;

    private static final Logger logger = LoggerFactory.getLogger(HistoryManagerImpl.class);

    private List<String> history = new ArrayList<String>();

    public HistoryManagerImpl() {
    }

    @Override
    public synchronized void saveHistory() {
        try {
            FileUtils.saveCommandHistoryString(history, new File(Constants.CMD_HISTORY_FILE));
        } catch (Throwable e) {
            logger.error("save command history failed", e);
        }
    }

    @Override
    public synchronized void loadHistory() {
        try {
            history = FileUtils.loadCommandHistoryString(new File(Constants.CMD_HISTORY_FILE));
        } catch (Throwable e) {
            logger.error("load command history failed", e);
        }
    }

    @Override
    public synchronized void clearHistory() {
        this.history.clear();
    }

    @Override
    public synchronized void addHistory(String commandLine) {
        while (history.size() >= MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        history.add(commandLine);
    }

    @Override
    public synchronized List<String> getHistory() {
        return new ArrayList<String>(history);
    }

    @Override
    public synchronized void setHistory(List<String> history) {
        this.history = history;
    }

}
