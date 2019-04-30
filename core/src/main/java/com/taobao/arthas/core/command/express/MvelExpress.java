package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

/**
 * @author xhinliang
 */
public class MvelExpress implements Express {

    private final Logger logger = LogUtil.getArthasLogger();

    private final MvelEvalKiller evalKiller;

    public MvelExpress() {
        evalKiller = new MvelEvalKiller();
    }

    @Override
    public Object get(String express) throws ExpressException {
        try {
            return evalKiller.eval(express);
        } catch (Exception e) {
            logger.error(null, "Error during evaluating the expression:", e);
            throw new ExpressException(express, e);
        }
    }

    @Override
    public boolean is(String express) throws ExpressException {
        final Object ret = get(express);
        return null != ret && ret instanceof Boolean && (Boolean) ret;
    }

    @Override
    public Express bind(Object object) {
        // TODO 现在没啥用...
        return this;
    }

    @Override
    public Express bind(String name, Object value) {
        evalKiller.getGlobalContext().put(name, value);
        return this;
    }

    @Override
    public Express reset() {
        evalKiller.getGlobalContext().clear();
        return this;
    }
}
