package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;

/**
 * 命令子类匹配器, 是AnnotatedCommand的子类并且不能标记为过期
 * @see Deprecated
 *
 * @author kaixinbaba 2020-09-11 17:24.
 */
public class AnnotatedCommandSubClassMatcher implements Matcher<Class<?>> {

    @Override
    public boolean matching(Class<?> target) {
        Deprecated deprecated = target.getAnnotation(Deprecated.class);
        return deprecated == null && AnnotatedCommand.class.isAssignableFrom(target);
    }
}
