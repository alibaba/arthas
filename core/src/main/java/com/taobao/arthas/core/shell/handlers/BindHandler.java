package com.taobao.arthas.core.shell.handlers;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ralf0131 2017-04-24 18:23.
 */
public class BindHandler implements Handler<Future<Void>> {

    private static final Logger logger = LoggerFactory.getLogger(BindHandler.class);

    private AtomicBoolean isBindRef;

    public BindHandler(AtomicBoolean isBindRef) {
        this.isBindRef = isBindRef;
    }

    @Override
    public void handle(Future<Void> event) {
        if (event.failed()) {
            logger.error("Error listening term server:", event.cause());
            isBindRef.compareAndSet(true, false);
        }
    }
}
