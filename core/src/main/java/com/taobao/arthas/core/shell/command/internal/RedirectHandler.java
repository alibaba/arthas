package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.LoggerFactory;
import org.slf4j.Logger;

/**
 * 重定向处理类
 *  
 * @author gehui 2017年7月27日 上午11:38:40
 */
public class RedirectHandler extends PlainTextHandler implements CloseFunction {

    private Logger logger = null;

    public RedirectHandler() {

    }

    public RedirectHandler(String name) {
        com.taobao.middleware.logger.Logger log = LoggerFactory.getLogger(name);
        log.activateAppenderWithSizeRolling("arthas-cache", name, "UTF-8", "200MB", 3);
        log.setAdditivity(false);
        log.activateAsync(128, -1);
        logger = (Logger) log.getDelegate();
    }

    @Override
    public String apply(String data) {
        data = super.apply(data);
        if (logger != null) {
            logger.info(data);
        } else {
            LogUtil.getResultLogger().info(data);
        }
        return data;
    }

    @Override
    public void close() {
        LogUtil.closeSlf4jLogger(logger);
    }
}
