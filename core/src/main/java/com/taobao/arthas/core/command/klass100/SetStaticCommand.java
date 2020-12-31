package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * 继承 GetStaticCommand做set实现
 */

@Name("setstatic")
@Summary("set the static field of a class")
@Description(Constants.EXAMPLE +
        "  setstatic -v '6666' demo.MathGame string \n" +
        "  setstatic -v '1024' demo.MathGame integer \n" +
        "  setstatic -v '{    \"arthas\":\"1024\",    \"setstatic\":\"777\"}'   demo.MathGame map\n" +
        "  setstatic -v '{    \"name\":\"bob\",    \"age\":\"18\"}'   demo.MathGame user\n" +
        "  setstatic -v 19  demo.MathGame user 'age'\n" +
        "  已知缺陷1: 对象的赋值是通过fastjson的parseObject(string,class)实现,需要属性有set方法或者fastjson注解.  或者通过ognl进行2次set实现\n" +
        "  已知缺陷2: 因为java值传递的问题, 表达式无法对集合数组等对象指定key/index直接赋值")
public class SetStaticCommand extends GetStaticCommand {

    private static final Logger logger = LoggerFactory.getLogger(SetStaticCommand.class);
    private String value = null;

    @Option(shortName = "v", longName = "value", required = true)
    @Description("set target field value as JSON value")

    public void setValue(String value) {
        this.value = value;
    }

//    private void processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst,
//                                   Set<Class<?>> matchedClasses) {
//        Matcher<String> fieldNameMatcher = fieldNameMatcher();
//
//        Class<?> clazz = matchedClasses.iterator().next();
//
//        boolean found = false;
//
//        for (Field field : clazz.getDeclaredFields()) {
//            if (!Modifier.isStatic(field.getModifiers()) || !fieldNameMatcher.matching(field.getName())) {
//                continue;
//            }
//            if (!field.isAccessible()) {
//                field.setAccessible(true);
//            }
//            try {
//                Object oldValue = field.get(null);
//                if (!StringUtils.isEmpty(express)) {
//                    //启用了OGNL表达式的场景下
//                    oldValue = ExpressFactory.threadLocalExpress(field.get(null)).get(express);
//                    Class type = oldValue.getClass();
//                    ExpressFactory.threadLocalExpress(field.get(null)).set(express, getValue(type));
//                } else {
//                    Class type = field.getType();
//                    reflectionSetValue(field, type);
//                }
//
//                String result = StringUtils.objectToString(expand >= 0 ? new ObjectView(oldValue, expand).draw() : oldValue);
//                process.write("field: " + field.getName() + "\nold" + result + "\n");
//                process.write("field: " + field.getName() + "\nnew\t" + value + "\n");
//
//                affect.rCnt(1);
//            } catch (IllegalAccessException e) {
//                logger.warn("setstatic: failed to get static value, class: {}, field: {} ", clazz, field.getName(), e);
//                process.write("Failed to get static, exception message: " + e.getMessage()
//                        + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
//            } catch (ExpressException e) {
//                logger.warn("setstatic: failed to get express value, class: {}, field: {}, express: {}", clazz, field.getName(), express, e);
//                process.write("Failed to get static, exception message: " + e.getMessage()
//                        + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
//            } catch (Exception e) {
//                process.write("set failed\n  field: " + field.getName() + "\t" + e.getMessage() + "\n");
//            } finally {
//                found = true;
//            }
//        }
//
//        if (!found) {
//            process.write("getstatic: no matched static field was found\n");
//        }
//    }

    /**
     * 基础数据类型:String/数值/布尔直接设置,  集合数组等对象通过json设置
     *
     * @param field
     * @param type
     * @throws IllegalAccessException
     */
    private void reflectionSetValue(Field field, Class type) throws IllegalAccessException {
        if (String.class.isAssignableFrom(type)) {
            field.set(null, value);
        } else if (Number.class.isAssignableFrom(type)) {
            if (Byte.class.isAssignableFrom(type)) {
                field.set(null, Byte.valueOf(value));
            } else if (Float.class.isAssignableFrom(type)) {
                field.set(null, Float.valueOf(value));
            } else if (Short.class.isAssignableFrom(type)) {
                field.set(null, Short.valueOf(value));
            } else if (Integer.class.isAssignableFrom(type)) {
                field.set(null, Integer.valueOf(value));
            } else if (Double.class.isAssignableFrom(type)) {
                field.set(null, Double.valueOf(value));
            } else if (Long.class.isAssignableFrom(type)) {
                field.set(null, Long.valueOf(value));
            }
        } else if (Boolean.class.isAssignableFrom(type)) {
            field.set(null, conversionValue(Boolean.class));
        } else {
            Object obj = JSONObject.parseObject(value, field.getType());
            field.set(null, obj);
        }
        throw new RuntimeException("unknown type:" + type.getName());
    }

    private Object conversionValue(Class type) throws IllegalAccessException {
        if (String.class.isAssignableFrom(type)) {
            return value;
        } else if (Number.class.isAssignableFrom(type)) {
            if (Byte.class.isAssignableFrom(type)) {
                return Byte.valueOf(value);
            } else if (Float.class.isAssignableFrom(type)) {
                return Float.valueOf(value);
            } else if (Short.class.isAssignableFrom(type)) {
                return Short.valueOf(value);
            } else if (Integer.class.isAssignableFrom(type)) {
                return Integer.valueOf(value);
            } else if (Double.class.isAssignableFrom(type)) {
                return Double.valueOf(value);
            } else if (Long.class.isAssignableFrom(type)) {
                return Long.valueOf(value);
            }
        } else if (Boolean.class.isAssignableFrom(type)) {
            try {
                return (Integer.valueOf(value) == 0);//兼容数字的情况
            } catch (Exception e) {
                return Boolean.valueOf(value);
            }
        } else {
            return JSONObject.parseObject(value, type);
        }
        throw new RuntimeException("unknown type:" + type.getName());
    }

    @Override
    public ExitStatus processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses) {
        Matcher<String> fieldNameMatcher = fieldNameMatcher();
        Class<?> clazz = matchedClasses.iterator().next();
        boolean found = false;
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !fieldNameMatcher.matching(field.getName())) {
                continue;
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                Object oldValue = field.get(null);
                if (!StringUtils.isEmpty(express)) {//启用了OGNL表达式的场景下
                    oldValue = ExpressFactory.threadLocalExpress(field.get(null)).get(express);
                    Class type = oldValue.getClass();
                    ExpressFactory.threadLocalExpress(field.get(null)).set(express, conversionValue(type));
                } else {
                    Class type = field.getType();
                    reflectionSetValue(field, type);
                }

                String result = StringUtils.objectToString(expand >= 0 ? new ObjectView(oldValue, expand).draw() : oldValue);
                process.write("field: " + field.getName() + "\nold value:" + result + "\n");
                process.write("field: " + field.getName() + "\nnew value:\t" + value + "\n");
                affect.rCnt(1);
            } catch (IllegalAccessException e) {
                logger.warn("setstatic: failed to set static value, class: {}, field: {} ", clazz, field.getName(), e);
                process.appendResult(new MessageModel("Failed to get static, exception message: " + e.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details. "));
            } catch (ExpressException e) {
                logger.warn("setstatic: failed to set express value, class: {}, field: {}, express: {}", clazz, field.getName(), express, e);
                process.appendResult(new MessageModel("Failed to get static, exception message: " + e.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details. "));
            } finally {
                found = true;
            }
        }

        if (!found) {
            return ExitStatus.failure(-1, "setstatic: no matched static field was found");
        } else {
            return ExitStatus.success();
        }
    }
}