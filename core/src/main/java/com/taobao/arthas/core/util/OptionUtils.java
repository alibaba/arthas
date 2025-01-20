package com.taobao.arthas.core.util;

import com.alibaba.bytekit.utils.AnnotationUtils;
import com.taobao.middleware.cli.annotations.Option;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class OptionUtils {

    public static List<Option> findOptions(Class<?> cmdClazz, Predicate<Option> filter) {
        if (cmdClazz == null) {
            return Collections.emptyList();
        }
        List<Option> options = new ArrayList<>();
        for (Method method : cmdClazz.getDeclaredMethods()) {
            Option option = AnnotationUtils.findAnnotation(method, Option.class);
            if (option != null && (filter == null || filter.test(option))) {
                options.add(option);
            }
        }
        return options;
    }

    public static List<Option> findNonFlagOptions(Class<?> cmdClazz) {
        return findOptions(cmdClazz, op -> !op.flag());
    }

    public static boolean containsShortOption(List<com.taobao.middleware.cli.annotations.Option> options, String name) {
        if (options == null || options.isEmpty()) {
            return false;
        }
        for (com.taobao.middleware.cli.annotations.Option option : options) {
            if (Objects.equals(option.shortName(), name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsLongOption(List<com.taobao.middleware.cli.annotations.Option> options, String name) {
        if (options == null || options.isEmpty()) {
            return false;
        }
        for (com.taobao.middleware.cli.annotations.Option option : options) {
            if (Objects.equals(option.longName(), name)) {
                return true;
            }
        }
        return false;
    }
}
