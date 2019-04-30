package com.taobao.arthas.core.command.express;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xhinliang
 */
public class MvelContext extends HashMap<String, Object> {

    private static final String GET_BEAN_BY_NAME = "getBeanByName";
    private static final String GET_BEAN_BY_CLASS = "getBeanByClass";
    private static final String GET_CLASS_BY_NAME = "getClassByName";

    static final Set<String> AUTO_LOAD_FUNCTIONS = new HashSet<String>();

    static  {
        AUTO_LOAD_FUNCTIONS.add(GET_BEAN_BY_NAME);
        AUTO_LOAD_FUNCTIONS.add(GET_BEAN_BY_CLASS);
        AUTO_LOAD_FUNCTIONS.add(GET_CLASS_BY_NAME);
    }

    private final MvelEvalKiller evalKiller;

    public MvelContext(MvelEvalKiller evalKiller) {
        this.evalKiller = evalKiller;
    }

    @Override
    public boolean containsKey(Object k) {
        if (k == null) {
            return false;
        }
        String key = (String) k;
        return super.containsKey(key) || getBean(key) != null;
    }

    @Override
    public Object get(Object k) {
        String key = (String) k;
        Object bean = super.get(key);
        if (bean == null) {
            bean = getBean(key);
        }
        return bean;
    }

    private Object getBean(String beanName) {
        if (AUTO_LOAD_FUNCTIONS.contains(beanName)) {
            return null;
        }
        Object bean = null;
        Class<?> clazz = null;
        if (this.containsKey(GET_BEAN_BY_NAME)) {
            bean = evalKiller.evalWithoutContext(String.format("%s(\"%s\")", GET_BEAN_BY_NAME, beanName));
        }
        if (bean == null) {
            try {
                String getClassEvalString = String.format("%s(\"%s\")", GET_CLASS_BY_NAME, beanName);
                if (this.containsKey(GET_CLASS_BY_NAME)) {
                    clazz = (Class<?>) evalKiller.evalWithoutContext(getClassEvalString);
                    if (this.containsKey(GET_BEAN_BY_CLASS)) {
                        bean = evalKiller.evalWithoutContext(String.format("%s(%s)", GET_BEAN_BY_CLASS, getClassEvalString));
                    }
                } else {
                    clazz = Class.forName(beanName);
                }
            } catch (Exception e) {
                // pass
            }
        }
        if (bean != null) {
            return bean;
        }
        if (clazz != null) {
            return clazz;
        }
        return null;
    }
}
