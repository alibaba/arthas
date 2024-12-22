package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.qlexpress4.ClassSupplier;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.runtime.ReflectLoader;
import com.alibaba.qlexpress4.security.QLSecurityStrategy;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.QLExpressConfigModel;


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
        initQLExpress(classResolver);
        initConfig();
        initContext();
    }

    private void initConfig() {
        try {
            if (GlobalOptions.QLExpressConfig.length() > 0) {
                QLOptions.Builder qlOptionsBuilder = QLOptions.builder();
                QLExpressConfigModel qlExpressConfigModel = JSON.parseObject(GlobalOptions.QLExpressConfig, QLExpressConfigModel.class);
                qlOptionsBuilder.cache(qlExpressConfigModel.isCache());
                qlOptionsBuilder.avoidNullPointer(qlExpressConfigModel.isAvoidNullPointer());
                qlOptionsBuilder.maxArrLength(qlExpressConfigModel.getMaxArrLength());
                qlOptionsBuilder.polluteUserContext(qlExpressConfigModel.isPolluteUserContext());
                qlOptionsBuilder.precise(qlExpressConfigModel.isPrecise());
                qlOptionsBuilder.timeoutMillis(qlExpressConfigModel.getTimeoutMillis());
                qlOptions = qlOptionsBuilder.build();
            }else {
                qlOptions = QLOptions.DEFAULT_OPTIONS;
            }
            //4.0设置InitOptions
        }catch (Throwable t){
            //异常不设置options
            logger.error("Error Init Options For QLExpress:", t);
        }
    }

    private void initQLExpress(ClassSupplier classResolver) {
        InitOptions.Builder initOptionsBuilder = InitOptions.builder();
        initOptionsBuilder.securityStrategy(QLSecurityStrategy.open());
        initOptionsBuilder.allowPrivateAccess(true);
        initOptionsBuilder.classSupplier(classResolver);
        initOptions = initOptionsBuilder.build();
        expressRunner = QLExpressRunner.getInstance(initOptions);
    }

    private void initContext() {
        ReflectLoader reflectLoader = new ReflectLoader(initOptions.getSecurityStrategy(), initOptions.getExtensionFunctions(), initOptions.isAllowPrivateAccess());
        qlGlobalContext = new QLGlobalContext(reflectLoader);
    }

    @Override
    public Object get(String express) throws ExpressException {
        try {
            Object result = expressRunner.execute(express, qlGlobalContext, qlOptions);
            return result;
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
