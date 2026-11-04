package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.term.impl.TermImpl;
import io.termd.core.function.Consumer;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class DefaultTermStdinHandler implements Consumer<int[]> {
    private TermImpl term;

    public DefaultTermStdinHandler(TermImpl term) {
        this.term = term;
    }

    @Override
    public void accept(int[] codePoints) {
        // Echo
        term.echo(codePoints);
        term.getReadline().queueEvent(codePoints);
    }
}
