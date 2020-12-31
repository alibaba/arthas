package com.taobao.arthas.core.command.express;

/**
 * 表达式
 * Created by vlinux on 15/5/20.
 */
public interface Express {

    /**
     * 根据表达式获取值
     *
     * @param express 表达式
     * @return 表达式运算后的值
     * @throws ExpressException 表达式运算出错
     */
    Object get(String express) throws ExpressException;

    /**
     * 根据表达式判断是与否
     *
     * @param express 表达式
     * @return 表达式运算后的布尔值
     * @throws ExpressException 表达式运算出错
     */
    boolean is(String express) throws ExpressException;

    /**
     * 设置表达式的值
     *
     * @param express 表达式
     * @param obj     表达式目标设置值
     * @return 成功/失败
     * @throws ExpressException
     */
    boolean set(String express, Object obj) throws ExpressException;

    /**
     * 绑定对象
     *
     * @param object 待绑定对象
     * @return this
     */
    Express bind(Object object);

    /**
     * 绑定变量
     *
     * @param name  变量名
     * @param value 变量值
     * @return this
     */
    Express bind(String name, Object value);

    /**
     * 重置整个表达式
     *
     * @return this
     */
    Express reset();


}
