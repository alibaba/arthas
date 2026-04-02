package com.taobao.arthas.core.util;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 类搜索工具
 * Created by vlinux on 15/5/17.
 * @author diecui1202 on 2017/09/07.
 */
public class SearchUtils {

    /**
     * 根据类名匹配，搜索已经被JVM加载的类
     *
     * @param inst             inst
     * @param classNameMatcher 类名匹配
     * @param limit            最大匹配限制
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClass(Instrumentation inst, Matcher<String> classNameMatcher, int limit) {
        if (classNameMatcher == null) {
            return Collections.emptySet();
        }
        final Set<Class<?>> matches = new HashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz == null) {
                continue;   
            }
            if (classNameMatcher.matching(clazz.getName())) {
                matches.add(clazz);
            }
            if (matches.size() >= limit) {
                break;
            }
        }
        return matches;
    }

    /**
     * 根据类名匹配，搜索已经被JVM加载的类（无限制）
     *
     * @param inst             Java Instrumentation 实例
     * @param classNameMatcher 类名匹配器
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClass(Instrumentation inst, Matcher<String> classNameMatcher) {
        return searchClass(inst, classNameMatcher, Integer.MAX_VALUE);
    }

    /**
     * 根据类名模式搜索已经被JVM加载的类
     *
     * @param inst        Java Instrumentation 实例
     * @param classPattern 类名模式（支持通配符或正则表达式）
     * @param isRegEx     是否为正则表达式（false表示通配符）
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClass(Instrumentation inst, String classPattern, boolean isRegEx) {
        Matcher<String> classNameMatcher = classNameMatcher(classPattern, isRegEx);
        return GlobalOptions.isDisableSubClass ? searchClass(inst, classNameMatcher) :
                searchSubClass(inst, searchClass(inst, classNameMatcher));
    }

    /**
     * 根据类名模式和类加载器哈希码搜索类
     *
     * @param inst        Java Instrumentation 实例
     * @param classPattern 类名模式（支持通配符或正则表达式）
     * @param isRegEx     是否为正则表达式（false表示通配符）
     * @param code        类加载器的哈希码（十六进制字符串）
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClass(Instrumentation inst, String classPattern, boolean isRegEx, String code) {
        Set<Class<?>> matchedClasses = searchClass(inst, classPattern, isRegEx);
        return filter(matchedClasses, code);
    }

    /**
     * 仅根据类名模式搜索类（不包含子类）
     *
     * @param inst        Java Instrumentation 实例
     * @param classPattern 类名模式（支持通配符或正则表达式）
     * @param isRegEx     是否为正则表达式（false表示通配符）
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClassOnly(Instrumentation inst, String classPattern, boolean isRegEx) {
        Matcher<String> classNameMatcher = classNameMatcher(classPattern, isRegEx);
        return searchClass(inst, classNameMatcher);
    }

    /**
     * 仅根据类名模式搜索类（不包含子类，限制结果数量）
     *
     * @param inst        Java Instrumentation 实例
     * @param classPattern 类名模式（通配符）
     * @param limit       最大匹配限制
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClassOnly(Instrumentation inst, String classPattern, int limit) {
        Matcher<String> classNameMatcher = classNameMatcher(classPattern, false);
        return searchClass(inst, classNameMatcher, limit);
    }

    /**
     * 仅根据类名模式和类加载器哈希码搜索类（不包含子类）
     *
     * @param inst        Java Instrumentation 实例
     * @param classPattern 类名模式（支持通配符或正则表达式）
     * @param isRegEx     是否为正则表达式（false表示通配符）
     * @param code        类加载器的哈希码（十六进制字符串）
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClassOnly(Instrumentation inst, String classPattern, boolean isRegEx, String code) {
        Set<Class<?>> matchedClasses = searchClassOnly(inst, classPattern, isRegEx);
        return filter(matchedClasses, code);
    }

    /**
     * 根据类加载器哈希码过滤类集合
     *
     * @param matchedClasses 已匹配的类集合
     * @param code          类加载器的哈希码（十六进制字符串）
     * @return 过滤后的类集合
     */
    private static Set<Class<?>> filter(Set<Class<?>> matchedClasses, String code) {
        if (code == null) {
            return matchedClasses;
        }

        Set<Class<?>> result = new HashSet<Class<?>>();
        if (matchedClasses != null) {
            for (Class<?> c : matchedClasses) {
                // 检查类的类加载器是否存在，并且其哈希码的十六进制表示是否与指定的code匹配
                if (c.getClassLoader() != null && Integer.toHexString(c.getClassLoader().hashCode()).equals(code)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     * 创建类名匹配器
     *
     * @param classPattern 类名模式
     * @param isRegEx     是否为正则表达式（false表示通配符）
     * @return 类名匹配器
     */
    public static Matcher<String> classNameMatcher(String classPattern, boolean isRegEx) {
        // 如果类名模式为空，则设置默认模式
        if (StringUtils.isEmpty(classPattern)) {
            classPattern = isRegEx ? ".*" : "*";
        }
        // 如果不是Lambda表达式，则将路径分隔符替换为点
        if (!classPattern.contains("$$Lambda")) {
            classPattern = StringUtils.replace(classPattern, "/", ".");
        }
        // 根据是否为正则表达式，返回相应的匹配器
        return isRegEx ? new RegexMatcher(classPattern) : new WildcardMatcher(classPattern);
    }

    /**
     * 搜索目标类的子类
     *
     * @param inst     inst
     * @param classSet 当前类集合
     * @return 匹配的子类集合
     */
    public static Set<Class<?>> searchSubClass(Instrumentation inst, Set<Class<?>> classSet) {
        final Set<Class<?>> matches = new HashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz == null) {
                continue;   
            }
            for (Class<?> superClass : classSet) {
                if (superClass.isAssignableFrom(clazz)) {
                    matches.add(clazz);
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * 搜索目标类的内部类
     *
     * @param inst inst
     * @param c    当前类
     * @return 匹配的类的集合
     */
    public static Set<Class<?>> searchInnerClass(Instrumentation inst, Class<?> c) {
        final Set<Class<?>> matches = new HashSet<Class<?>>();
        for (Class<?> clazz : inst.getInitiatedClasses(c.getClassLoader())) {
            if (c.getClassLoader() != null && clazz.getClassLoader() != null && c.getClassLoader().equals(clazz.getClassLoader())) {
                if (clazz.getName().startsWith(c.getName())) {
                    matches.add(clazz);
                }
            }
        }
        return matches;
    }
}
