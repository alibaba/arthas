package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.qlexpress4.*;
import com.alibaba.qlexpress4.security.QLSecurityStrategy;


/**
 * @Author TaoKan
 * @Date 2024/9/17 6:01 PM
 */
public class QLExpress implements Express {
    private static final Logger logger = LoggerFactory.getLogger(QLExpress.class);
    private Express4Runner expressRunner;
    private QLGlobalContext qlGlobalContext;

    private QLOptions qlOptions;

    private InitOptions initOptions;

    public QLExpress() {
        this(QLExpressCustomClassResolver.customClassResolver);
    }

    public QLExpress(ClassSupplier classResolver) {
        this.initOptions = initQLExpress(classResolver);
        this.expressRunner = QLExpressRunner.getInstance(initOptions);
        this.qlOptions = initConfig();
        this.qlGlobalContext = new QLGlobalContext(expressRunner);
    }

    private QLOptions initConfig() {
        return QLOptions.DEFAULT_OPTIONS;
    }

    private InitOptions initQLExpress(ClassSupplier classResolver) {
        InitOptions.Builder initOptionsBuilder = InitOptions.builder();
        initOptionsBuilder.securityStrategy(QLSecurityStrategy.open());
        initOptionsBuilder.allowPrivateAccess(true);
        initOptionsBuilder.classSupplier(classResolver);
        return initOptionsBuilder.build();
    }

    @Override
    public Object get(String express) throws ExpressException {
        try {
            return expressRunner.execute(express, qlGlobalContext, qlOptions).getResult();
        } catch (Exception e) {
            logger.error("Error during evaluating the expression with QLExpress:", e);
            throw new ExpressException(express, e);
        }
    }

    @Override
    public boolean is(String express) throws ExpressException {
        final Object ret = get(express);
        return ret instanceof Boolean && (Boolean) ret;
    }

    @Override
    public Express bind(Object object) {
        qlGlobalContext.bindObj(object);
        return this;
    }

    @Override
    public Express bind(String name, Object value) {
        qlGlobalContext.put(name, value);
        return this;
    }

    @Override
    public Express reset() {
        qlGlobalContext.clear();
        return this;
    }
}
