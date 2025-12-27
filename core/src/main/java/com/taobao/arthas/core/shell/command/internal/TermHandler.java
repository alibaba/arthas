package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.term.Term;
import io.netty.util.internal.InternalThreadLocalMap;

/**
 * 将数据写到term
 * 
 * @author gehui 2017年7月26日 上午11:20:00
 */
public class TermHandler extends StdoutHandler {
    private Term term;

    public TermHandler(Term term) {
        this.term = term;
    }

    @Override
    public String apply(String data) {
        try {
            term.write(data);
        } finally {
            // Termd 基于 Netty，业务线程输出时会创建 Netty 的 ThreadLocal（InternalThreadLocalMap），
            // stop/detach 后残留在业务线程的 ThreadLocalMap.table 会导致 ArthasClassLoader 无法回收。
            try {
                InternalThreadLocalMap.remove();
            } catch (Throwable t) {
                // ignore
            }
        }
        return data;
    }
}
