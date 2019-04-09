package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.ThreadUtil;
import com.taobao.arthas.core.view.TreeView;

/**
 * 用于在ThreadLocal中传递的实体
 * @author ralf0131 2017-01-05 14:05.
 */
public class TraceEntity {

    protected TreeView view;
    protected int deep;

    public TraceEntity() {
        this.view = createTreeView();
        this.deep = 0;
    }

    public TreeView getView() {
        return view;
    }

    public void setView(TreeView view) {
        this.view = view;
    }

    public int getDeep() {
        return deep;
    }

    public void setDeep(int deep) {
        this.deep = deep;
    }

    private TreeView createTreeView() {
        String threadTitle = "ts=" + DateUtils.getCurrentDate()+ ";" + ThreadUtil.getThreadTitle(Thread.currentThread());
        return new TreeView(true, threadTitle);
    }
}
