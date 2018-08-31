package com.taobao.arthas.core.shell.handlers;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

/**
 * @author beiwei30 on 22/11/2016.
 */
public class NoOpHandler implements Handler {

    private static final Logger logger = LogUtil.getArthasLogger();

    @Override
    public void handle(Object event) {
        if (event instanceof Future && ((Future) event).failed()) {
            logger.error(null, "Error listening term server:", ((Future) event).cause());
        }
    }
}
