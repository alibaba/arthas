package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

/**
 * 
 * @author qxo 2018-12-01
 *
 */
public abstract class AbstractExpress implements Express {
	
	protected Logger logger = LogUtil.getArthasLogger();
	protected Object bindObject;
	
    @Override
    public final boolean is(String express) throws ExpressException {
        final Object ret = get(express);
        return null != ret && ret instanceof Boolean && (Boolean) ret;
    }

    @Override
    public final Express bind(Object object) {
        this.bindObject = object;
        return this;
    }
}
