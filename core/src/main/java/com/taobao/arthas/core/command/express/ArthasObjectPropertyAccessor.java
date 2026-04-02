package com.taobao.arthas.core.command.express;

import java.util.Map;

import com.taobao.arthas.core.GlobalOptions;

import ognl.ObjectPropertyAccessor;
import ognl.OgnlException;

/**
 * Arthas对象属性访问器
 *
 * 该类继承自OGNL的ObjectPropertyAccessor，用于在Arthas中使用OGNL表达式时对对象属性的访问进行控制。
 * 主要作用是在严格模式下禁止通过OGNL表达式修改对象的属性，以增强安全性。
 *
 * @author hengyunabc 2022-03-24
 */
public class ArthasObjectPropertyAccessor extends ObjectPropertyAccessor {

    /**
     * 设置对象的可能属性
     *
     * 该方法在OGNL表达式尝试设置对象属性时被调用。
     * 在严格模式下，会抛出IllegalAccessError异常阻止属性修改。
     *
     * @param context OGNL上下文对象，包含表达式执行时的环境信息
     * @param target 目标对象，其属性将被设置
     * @param name 要设置的属性名称
     * @param value 要设置的属性值
     * @return 返回父类方法的执行结果
     * @throws OgnlException 如果OGNL表达式执行过程中出现错误
     * @throws IllegalAccessError 如果启用了严格模式（strict），则抛出此异常阻止属性修改
     */
    @Override
    public Object setPossibleProperty(Map context, Object target, String name, Object value) throws OgnlException {
        // 检查是否启用了严格模式
        if (GlobalOptions.strict) {
            // 严格模式下禁止修改对象属性，抛出非法访问错误
            throw new IllegalAccessError(GlobalOptions.STRICT_MESSAGE);
        }
        // 非严格模式下，调用父类方法正常设置属性
        return super.setPossibleProperty(context, target, name, value);
    }

}
