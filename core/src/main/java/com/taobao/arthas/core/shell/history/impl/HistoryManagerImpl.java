package com.taobao.arthas.core.shell.history.impl;

import com.taobao.arthas.core.shell.history.HistoryManager;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/4/8
 */
public class HistoryManagerImpl implements HistoryManager {
    /**
     * The max number of history item that will be saved in memory.
     */
    private static final int MAX_HISTORY_SIZE = 500;

    private List<String> history = new ArrayList<String>();

    public HistoryManagerImpl() {
    }

    @Override
    public void saveHistory() {
        FileUtils.saveCommandHistoryString(history, new File(Constants.CMD_HISTORY_FILE));
    }

    @Override
    public void loadHistory() {
        history = FileUtils.loadCommandHistoryString(new File(Constants.CMD_HISTORY_FILE));
    }

    @Override
    public void clearHistory() {
        this.history.clear();
    }

    @Override
    public void addHistory(String commandLine) {
        while (history.size() >= MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        history.add(commandLine);
    }

    @Override
    public List<String> getHistory() {
        return history;
    }

    @Override
    public void setHistory(List<String> history) {
        this.history = history;
    }


}
