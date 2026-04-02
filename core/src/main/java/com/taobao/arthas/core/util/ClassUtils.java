package com.taobao.arthas.core.util;

import static com.taobao.text.ui.Element.label;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.MethodVO;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;

import static com.taobao.text.Decoration.bold;

/**
 * 类工具类
 * 提供类信息的创建、渲染和转换功能
 *
 * @author hengyunabc 2018-10-18
 *
 */
public class ClassUtils {

    /**
     * 获取代码源位置
     * 从CodeSource对象中提取类文件的位置信息
     *
     * @param cs CodeSource对象，可能为null
     * @return 代码源文件路径，如果无法获取则返回空字符串
     */
    public static String getCodeSource(final CodeSource cs) {
        // 检查CodeSource及其Location是否为null
        if (null == cs || null == cs.getLocation() || null == cs.getLocation().getFile()) {
            return com.taobao.arthas.core.util.Constants.EMPTY_STRING;
        }

        // 返回代码源位置的文件路径
        return cs.getLocation().getFile();
    }

    /**
     * 判断一个类是否是Lambda表达式生成的类
     * Lambda类在运行时会生成包含"$$Lambda"的特殊类名
     *
     * @param clazz 要检查的类
     * @return 如果是Lambda类返回true，否则返回false
     */
    public static boolean isLambdaClass(Class<?> clazz) {
        // Lambda类的类名包含"$$Lambda"字符串
        return clazz.getName().contains("$$Lambda");
    }

    /**
     * 渲染类信息（不包含字段）
     *
     * @param clazz 类详细信息对象
     * @return 渲染后的表格元素
     */
    public static Element renderClassInfo(ClassDetailVO clazz) {
        return renderClassInfo(clazz, false);
    }

    /**
     * 渲染类详细信息
     * 将类的各种信息以表格形式展示，包括类名、修饰符、继承关系、类加载器等
     *
     * @param clazz        类详细信息对象
     * @param isPrintField 是否打印字段信息
     * @return 渲染后的表格元素
     */
    public static Element renderClassInfo(ClassDetailVO clazz, boolean isPrintField) {
        // 创建表格，设置左右内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);

        // 添加类的各种属性信息到表格中
        table.row(label("class-info").style(Decoration.bold.bold()), label(clazz.getClassInfo()))
                .row(label("code-source").style(Decoration.bold.bold()), label(clazz.getCodeSource()))
                .row(label("name").style(Decoration.bold.bold()), label(clazz.getName()))
                .row(label("isInterface").style(Decoration.bold.bold()), label("" + clazz.isInterface()))
                .row(label("isAnnotation").style(Decoration.bold.bold()), label("" + clazz.isAnnotation()))
                .row(label("isEnum").style(Decoration.bold.bold()), label("" + clazz.isEnum()))
                .row(label("isAnonymousClass").style(Decoration.bold.bold()), label("" + clazz.isAnonymousClass()))
                .row(label("isArray").style(Decoration.bold.bold()), label("" + clazz.isArray()))
                .row(label("isLocalClass").style(Decoration.bold.bold()), label("" + clazz.isLocalClass()))
                .row(label("isMemberClass").style(Decoration.bold.bold()), label("" + clazz.isMemberClass()))
                .row(label("isPrimitive").style(Decoration.bold.bold()), label("" + clazz.isPrimitive()))
                .row(label("isSynthetic").style(Decoration.bold.bold()), label("" + clazz.isSynthetic()))
                .row(label("simple-name").style(Decoration.bold.bold()), label(clazz.getSimpleName()))
                .row(label("modifier").style(Decoration.bold.bold()), label(clazz.getModifier()))
                .row(label("annotation").style(Decoration.bold.bold()), label(StringUtils.join(clazz.getAnnotations(), ",")))
                .row(label("interfaces").style(Decoration.bold.bold()), label(StringUtils.join(clazz.getInterfaces(), ",")))
                .row(label("super-class").style(Decoration.bold.bold()), TypeRenderUtils.drawSuperClass(clazz))
                .row(label("class-loader").style(Decoration.bold.bold()), TypeRenderUtils.drawClassLoader(clazz))
                .row(label("classLoaderHash").style(Decoration.bold.bold()), label(clazz.getClassLoaderHash()));

        // 如果需要打印字段信息，添加字段行
        if (isPrintField) {
            table.row(label("fields").style(Decoration.bold.bold()), TypeRenderUtils.drawField(clazz));
        }
        return table;
    }

    /**
     * 创建类详细信息对象
     * 从Class对象中提取完整的信息并封装到ClassDetailVO中
     *
     * @param clazz     要分析的类
     * @param withFields 是否包含字段信息
     * @param expand    字段展开的层级限制
     * @return 类详细信息对象
     */
    public static ClassDetailVO createClassInfo(Class clazz, boolean withFields, Integer expand) {
        // 获取代码源位置
        CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        ClassDetailVO classInfo = new ClassDetailVO();

        // 设置基本信息
        classInfo.setName(StringUtils.classname(clazz));
        classInfo.setClassInfo(StringUtils.classname(clazz));
        classInfo.setCodeSource(ClassUtils.getCodeSource(cs));

        // 设置类类型标志
        classInfo.setInterface(clazz.isInterface());
        classInfo.setAnnotation(clazz.isAnnotation());
        classInfo.setEnum(clazz.isEnum());
        classInfo.setAnonymousClass(clazz.isAnonymousClass());
        classInfo.setArray(clazz.isArray());
        classInfo.setLocalClass(clazz.isLocalClass());
        classInfo.setMemberClass(clazz.isMemberClass());
        classInfo.setPrimitive(clazz.isPrimitive());
        classInfo.setSynthetic(clazz.isSynthetic());

        // 设置类名和修饰符
        classInfo.setSimpleName(clazz.getSimpleName());
        classInfo.setModifier(StringUtils.modifier(clazz.getModifiers(), ','));

        // 设置继承和实现关系
        classInfo.setAnnotations(TypeRenderUtils.getAnnotations(clazz));
        classInfo.setInterfaces(TypeRenderUtils.getInterfaces(clazz));
        classInfo.setSuperClass(TypeRenderUtils.getSuperClass(clazz));

        // 设置类加载器信息
        classInfo.setClassloader(TypeRenderUtils.getClassloader(clazz));
        classInfo.setClassLoaderHash(StringUtils.classLoaderHash(clazz));

        // 如果需要，添加字段信息
        if (withFields) {
            classInfo.setFields(TypeRenderUtils.getFields(clazz, expand));
        }
        return classInfo;
    }

    /**
     * 创建简单类信息对象
     * 只包含类名、类加载器和类加载器哈希值
     *
     * @param clazz 要分析的类
     * @return 简单类信息对象
     */
    public static ClassVO createSimpleClassInfo(Class clazz) {
        ClassVO classInfo = new ClassVO();
        fillSimpleClassVO(clazz, classInfo);
        return classInfo;
    }

    /**
     * 填充简单类信息对象
     * 将类的基本信息填充到ClassVO对象中
     *
     * @param clazz    要分析的类
     * @param classInfo 要填充的类信息对象
     */
    public static void fillSimpleClassVO(Class clazz, ClassVO classInfo) {
        classInfo.setName(StringUtils.classname(clazz));
        classInfo.setClassloader(TypeRenderUtils.getClassloader(clazz));
        classInfo.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
    }

    /**
     * 创建方法信息对象
     * 从Method对象中提取方法信息
     *
     * @param method 要分析的方法
     * @param clazz  方法所在的类
     * @param detail 是否包含详细信息（修饰符、注解、参数、返回值、异常等）
     * @return 方法信息对象
     */
    public static MethodVO createMethodInfo(Method method, Class clazz, boolean detail) {
        MethodVO methodVO = new MethodVO();
        // 设置基本方法信息
        methodVO.setDeclaringClass(clazz.getName());
        methodVO.setMethodName(method.getName());
        methodVO.setDescriptor(Type.getMethodDescriptor(method));
        methodVO.setConstructor(false);

        // 如果需要详细信息
        if (detail) {
            methodVO.setModifier(StringUtils.modifier(method.getModifiers(), ','));
            methodVO.setAnnotations(TypeRenderUtils.getAnnotations(method.getDeclaredAnnotations()));
            methodVO.setParameters(getClassNameList(method.getParameterTypes()));
            methodVO.setReturnType(StringUtils.classname(method.getReturnType()));
            methodVO.setExceptions(getClassNameList(method.getExceptionTypes()));
            methodVO.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
        }
        return methodVO;
    }

    /**
     * 创建构造方法信息对象
     * 从Constructor对象中提取构造方法信息
     *
     * @param constructor 要分析的构造方法
     * @param clazz       构造方法所在的类
     * @param detail      是否包含详细信息（修饰符、注解、参数、异常等）
     * @return 方法信息对象（构造方法被特殊标记）
     */
    public static MethodVO createMethodInfo(Constructor constructor, Class clazz, boolean detail) {
        MethodVO methodVO = new MethodVO();
        // 设置基本构造方法信息
        methodVO.setDeclaringClass(clazz.getName());
        methodVO.setDescriptor(Type.getConstructorDescriptor(constructor));
        methodVO.setMethodName("<init>");  // 构造方法的方法名固定为<init>
        methodVO.setConstructor(true);     // 标记为构造方法

        // 如果需要详细信息
        if (detail) {
            methodVO.setModifier(StringUtils.modifier(constructor.getModifiers(), ','));
            methodVO.setAnnotations(TypeRenderUtils.getAnnotations(constructor.getDeclaredAnnotations()));
            methodVO.setParameters(getClassNameList(constructor.getParameterTypes()));
            methodVO.setExceptions(getClassNameList(constructor.getExceptionTypes()));
            methodVO.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
        }
        return methodVO;
    }

    /**
     * 渲染方法信息
     * 将方法的各种信息以表格形式展示
     *
     * @param method 方法信息对象
     * @return 渲染后的表格元素
     */
    public static Element renderMethod(MethodVO method) {
        // 创建表格，设置左右内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加方法的各种信息到表格中
        table.row(label("declaring-class").style(bold.bold()), label(method.getDeclaringClass()))
                .row(label("method-name").style(bold.bold()), label(method.getMethodName()).style(bold.bold()))
                .row(label("modifier").style(bold.bold()), label(method.getModifier()))
                .row(label("annotation").style(bold.bold()), label(TypeRenderUtils.drawAnnotation(method.getAnnotations())))
                .row(label("parameters").style(bold.bold()), label(TypeRenderUtils.drawParameters(method.getParameters())))
                .row(label("return").style(bold.bold()), label(method.getReturnType()))
                .row(label("exceptions").style(bold.bold()), label(TypeRenderUtils.drawExceptions(method.getExceptions())))
                .row(label("classLoaderHash").style(bold.bold()), label(method.getClassLoaderHash()));
        return table;
    }

    /**
     * 渲染构造方法信息
     * 将构造方法的各种信息以表格形式展示
     *
     * @param constructor 构造方法信息对象
     * @return 渲染后的表格元素
     */
    public static Element renderConstructor(MethodVO constructor) {
        // 创建表格，设置左右内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加构造方法的各种信息到表格中
        table.row(label("declaring-class").style(bold.bold()), label(constructor.getDeclaringClass()))
                .row(label("constructor-name").style(bold.bold()), label("<init>").style(bold.bold()))
                .row(label("modifier").style(bold.bold()), label(constructor.getModifier()))
                .row(label("annotation").style(bold.bold()), label(TypeRenderUtils.drawAnnotation(constructor.getAnnotations())))
                .row(label("parameters").style(bold.bold()), label(TypeRenderUtils.drawParameters(constructor.getParameters())))
                .row(label("exceptions").style(bold.bold()), label(TypeRenderUtils.drawExceptions(constructor.getExceptions())))
                .row(label("classLoaderHash").style(bold.bold()), label(constructor.getClassLoaderHash()));
        return table;
    }

    /**
     * 将类数组转换为类名字符串数组
     * 用于获取接口、参数类型、异常类型的类名列表
     *
     * @param classes 类数组
     * @return 类名字符串数组
     */
    public static String[] getClassNameList(Class[] classes) {
        List<String> list = new ArrayList<String>();
        // 遍历类数组，提取每个类的名称
        for (Class anInterface : classes) {
            list.add(StringUtils.classname(anInterface));
        }
        return list.toArray(new String[0]);
    }

    /**
     * 创建类信息对象列表
     * 将类的集合转换为ClassVO对象列表
     *
     * @param matchedClasses 匹配的类集合
     * @return 类信息对象列表
     */
    public static List<ClassVO> createClassVOList(Collection<Class<?>> matchedClasses) {
        List<ClassVO> classVOs = new ArrayList<ClassVO>(matchedClasses.size());
        // 遍历类集合，为每个类创建简单信息对象
        for (Class<?> aClass : matchedClasses) {
            ClassVO classVO = createSimpleClassInfo(aClass);
            classVOs.add(classVO);
        }
        return classVOs;
    }

    /**
     * 创建类加载器信息对象
     * 将ClassLoader转换为ClassLoaderVO对象
     *
     * @param classLoader 类加载器实例
     * @return 类加载器信息对象
     */
    public static ClassLoaderVO createClassLoaderVO(ClassLoader classLoader) {
        ClassLoaderVO classLoaderVO = new ClassLoaderVO();
        // 设置类加载器的哈希值
        classLoaderVO.setHash(classLoaderHash(classLoader));
        // 设置类加载器名称，如果是null则显示为BootstrapClassLoader
        classLoaderVO.setName(classLoader==null?"BootstrapClassLoader":classLoader.toString());
        // 设置父类加载器
        ClassLoader parent = classLoader == null ? null : classLoader.getParent();
        classLoaderVO.setParent(parent==null?null:parent.toString());
        return classLoaderVO;
    }

    /**
     * 创建类加载器信息对象列表
     * 将类加载器的集合转换为ClassLoaderVO对象列表
     *
     * @param classLoaders 类加载器集合
     * @return 类加载器信息对象列表
     */
    public static List<ClassLoaderVO> createClassLoaderVOList(Collection<ClassLoader> classLoaders) {
        List<ClassLoaderVO> classLoaderVOList = new ArrayList<ClassLoaderVO>();
        // 遍历类加载器集合，为每个类加载器创建信息对象
        for (ClassLoader classLoader : classLoaders) {
            classLoaderVOList.add(createClassLoaderVO(classLoader));
        }
        return classLoaderVOList;
    }

    /**
     * 获取类的类加载器哈希值
     *
     * @param clazz 类对象
     * @return 类加载器的十六进制哈希字符串，如果是Bootstrap ClassLoader则返回"null"
     */
    public static String classLoaderHash(Class<?> clazz) {
        // 检查类或类加载器是否为null
        if (clazz == null || clazz.getClassLoader() == null) {
            return "null";
        }
        // 返回类加载器的十六进制哈希值
        return Integer.toHexString(clazz.getClassLoader().hashCode());
    }

    /**
     * 获取类加载器的哈希值
     *
     * @param classLoader 类加载器实例
     * @return 类加载器的十六进制哈希字符串，如果是null则返回"null"
     */
    public static String classLoaderHash(ClassLoader classLoader) {
        if (classLoader == null ) {
            return "null";
        }
        return Integer.toHexString(classLoader.hashCode());
    }

    /**
     * 渲染匹配的类列表
     * 将匹配的类以表格形式展示，包含类名、哈希值和类加载器信息
     *
     * @param matchedClasses 匹配的类集合
     * @return 渲染后的表格元素
     */
    public static Element renderMatchedClasses(Collection<ClassVO> matchedClasses) {
        // 创建表格，设置左右内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头
        table.row(new LabelElement("NAME").style(Decoration.bold.bold()),
                new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()));

        // 遍历类集合，添加每一行的数据
        for (ClassVO c : matchedClasses) {
            table.row(label(c.getName()),
                    label(c.getClassLoaderHash()).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(c));
        }
        return table;
    }

}
