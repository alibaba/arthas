package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.term.impl.TermImpl;
import io.termd.core.function.BiConsumer;
import io.termd.core.tty.TtyEvent;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class EventHandler implements BiConsumer<TtyEvent, Integer> {
    private TermImpl term;

    public EventHandler(TermImpl term) {
        this.term = term;
    }

    @Override
    public void accept(TtyEvent event, Integer key) {
        switch (event) {
            case INTR:
                term.handleIntr(key);
                break;
            case EOF:
                term.handleEof(key);
                break;
            case SUSP:
                term.handleSusp(key);
                break;
        }
    }
}
