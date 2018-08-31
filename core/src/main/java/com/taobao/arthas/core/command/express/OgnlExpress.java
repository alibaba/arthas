package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;
import ognl.DefaultMemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * @author ralf0131 2017-01-04 14:41.
 */
public class OgnlExpress implements Express {

    Logger logger = LogUtil.getArthasLogger();

    private Object bindObject;
    private final OgnlContext context;

    public OgnlExpress() {
        context = new OgnlContext();
        context.setClassResolver(CustomClassResolver.customClassResolver);
    }

    @Override
    public Object get(String express) throws ExpressException {
        try {
            context.setMemberAccess(new DefaultMemberAccess(true));
            return Ognl.getValue(express, context, bindObject);
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
        this.bindObject = object;
        return this;
    }

    @Override
    public Express bind(String name, Object value) {
        context.put(name, value);
        return this;
    }

    @Override
    public Express reset() {
        context.clear();
        context.setClassResolver(CustomClassResolver.customClassResolver);
        return this;
    }
}
