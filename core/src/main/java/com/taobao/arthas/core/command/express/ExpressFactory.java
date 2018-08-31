package com.taobao.arthas.core.command.express;

/**
 * 表达式工厂类
 * @author ralf0131 2017-01-04 14:40.
 */
public class ExpressFactory {

    private static final ThreadLocal<Express> expressRef = new ThreadLocal<Express>() {
        @Override
        protected Express initialValue() {
            return new OgnlExpress();
        }
    };

    /**
     * 构造表达式执行类
     *
     * @param object 执行对象
     * @return 返回表达式实现
     */
    public static Express newExpress(Object object) {
        return expressRef.get().reset().bind(object);
    }

}