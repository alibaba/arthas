package com.taobao.arthas.core.command.express;

import ognl.ClassResolver;
import ognl.DefaultMemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * @author ralf0131 2017-01-04 14:41.
 * @author hengyunabc 2018-10-18
 */
public class OgnlExpress extends AbstractExpress {


    private final OgnlContext context;

    public OgnlExpress() {
        this(CustomClassResolver.customClassResolver);
    }

    public OgnlExpress(ClassResolver classResolver) {
        context = new OgnlContext();
        context.setClassResolver(classResolver);
        // allow private field access
        context.setMemberAccess(new DefaultMemberAccess(true));
    }

    @Override
    public Object get(String express) throws ExpressException {
        try {
            return Ognl.getValue(express, context, bindObject);
        } catch (Exception e) {
            logger.error(null, "Error during evaluating the expression:", e);
            throw new ExpressException(express, e);
        }
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
