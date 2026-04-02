package com.taobao.arthas.core.util;

import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.FieldVO;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.ui.TreeElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * 类型渲染工具类
 * <p>
 * 提供将Java类型信息（类、方法、字段等）转换为可视化展示格式的工具方法。
 * 主要用于将反射获取的类型信息转换为用户友好的展示形式。
 * </p>
 *
 * @author beiwei30 on 24/11/2016.
 */
public class TypeRenderUtils {

    /**
     * 渲染类实现的接口列表
     * <p>
     * 将类实现的所有接口用逗号连接成一个字符串。
     * </p>
     *
     * @param clazz 要渲染的类
     * @return 接口列表字符串，多个接口用逗号分隔
     */
    public static String drawInterface(Class<?> clazz) {
        return StringUtils.concat(",", clazz.getInterfaces());
    }

    /**
     * 渲染方法的参数列表
     * <p>
     * 将方法的所有参数类型用换行符连接，每个参数类型占一行。
     * </p>
     *
     * @param method 要渲染的方法
     * @return 参数类型列表字符串，多个参数用换行符分隔
     */
    public static String drawParameters(Method method) {
        return StringUtils.concat("\n", method.getParameterTypes());
    }

    /**
     * 渲染构造器的参数列表
     * <p>
     * 将构造器的所有参数类型用换行符连接，每个参数类型占一行。
     * </p>
     *
     * @param constructor 要渲染的构造器
     * @return 参数类型列表字符串，多个参数用换行符分隔
     */
    public static String drawParameters(Constructor constructor) {
        return StringUtils.concat("\n", constructor.getParameterTypes());
    }

    /**
     * 渲染参数类型数组
     * <p>
     * 将参数类型数组用换行符连接成一个字符串。
     * </p>
     *
     * @param parameterTypes 参数类型数组
     * @return 参数类型列表字符串，多个参数用换行符分隔
     */
    public static String drawParameters(String[] parameterTypes) {
        return StringUtils.concat("\n", parameterTypes);
    }

    /**
     * 渲染方法的返回类型
     * <p>
     * 获取方法返回类型的简单类名表示。
     * </p>
     *
     * @param method 要渲染的方法
     * @return 返回类型的类名
     */
    public static String drawReturn(Method method) {
        return StringUtils.classname(method.getReturnType());
    }

    /**
     * 渲染方法抛出的异常列表
     * <p>
     * 将方法声明抛出的所有异常类型用换行符连接。
     * </p>
     *
     * @param method 要渲染的方法
     * @return 异常类型列表字符串，多个异常用换行符分隔
     */
    public static String drawExceptions(Method method) {
        return StringUtils.concat("\n", method.getExceptionTypes());
    }

    /**
     * 渲染构造器抛出的异常列表
     * <p>
     * 将构造器声明抛出的所有异常类型用换行符连接。
     * </p>
     *
     * @param constructor 要渲染的构造器
     * @return 异常类型列表字符串，多个异常用换行符分隔
     */
    public static String drawExceptions(Constructor constructor) {
        return StringUtils.concat("\n", constructor.getExceptionTypes());
    }

    /**
     * 渲染异常类型数组
     * <p>
     * 将异常类型数组用换行符连接成一个字符串。
     * </p>
     *
     * @param exceptionTypes 异常类型数组
     * @return 异常类型列表字符串，多个异常用换行符分隔
     */
    public static String drawExceptions(String[] exceptionTypes) {
        return StringUtils.concat("\n", exceptionTypes);
    }

    /**
     * 渲染父类继承树
     * <p>
     * 将类的继承关系渲染为树形结构。
     * </p>
     *
     * @param clazz 包含父类信息的类详情对象
     * @return 树形结构的元素
     */
    public static Element drawSuperClass(ClassDetailVO clazz) {
        return drawTree(clazz.getSuperClass());
    }

    /**
     * 渲染类加载器层次结构
     * <p>
     * 将类的类加载器及其父加载器渲染为树形结构。
     * </p>
     *
     * @param clazz 包含类加载器信息的类对象
     * @return 树形结构的元素
     */
    public static Element drawClassLoader(ClassVO clazz) {
        String[] classloaders = clazz.getClassloader();
        return drawTree(classloaders);
    }

    /**
     * 将字符串数组渲染为树形结构
     * <p>
     * 数组中的每个元素作为树的一层，形成链式树结构。
     * 常用于显示继承链或类加载器层次结构。
     * </p>
     *
     * @param nodes 节点字符串数组
     * @return 树形结构的根元素
     */
    public static Element drawTree(String[] nodes) {
        TreeElement root = new TreeElement();
        TreeElement parent = root;
        // 将每个节点添加为前一个节点的子节点，形成链式结构
        for (String node : nodes) {
            TreeElement child = new TreeElement(label(node));
            parent.addChild(child);
            parent = child;
        }
        return root;
    }

    /**
     * 渲染类的字段信息
     * <p>
     * 将类的所有字段渲染为表格形式，包含字段名、类型、修饰符、注解等信息。
     * 对于静态字段，还会显示其当前值。
     * </p>
     *
     * @param clazz 包含字段信息的类详情对象
     * @return 包含所有字段信息的表格元素
     */
    public static Element drawField(ClassDetailVO clazz) {
        TableElement fieldsTable = new TableElement(1).leftCellPadding(0).rightCellPadding(0);
        FieldVO[] fields = clazz.getFields();
        if (fields == null || fields.length == 0) {
            return fieldsTable;
        }

        // 遍历所有字段，为每个字段创建一个子表格
        for (FieldVO field : fields) {
            TableElement fieldTable = new TableElement().leftCellPadding(0).rightCellPadding(1);
            // 添加字段的基本信息
            fieldTable.row("name", field.getName())
                    .row("type", field.getType())
                    .row("modifier", field.getModifier());

            // 如果字段有注解，添加注解信息
            String[] annotations = field.getAnnotations();
            if (annotations != null && annotations.length > 0) {
                fieldTable.row("annotation", drawAnnotation(annotations));
            }

            // 如果是静态字段，显示其当前值
            if (field.isStatic()) {
                ObjectVO objectVO = field.getValue();
                Object o = objectVO.needExpand() ? new ObjectView(objectVO).draw() : objectVO.getObject();
                fieldTable.row("value", StringUtils.objectToString(o));
            }

            // 添加空行作为分隔
            fieldTable.row(label(""));
            fieldsTable.row(fieldTable);
        }

        return fieldsTable;
    }

    /**
     * 渲染注解列表
     * <p>
     * 将多个注解用逗号连接成一个字符串。
     * </p>
     *
     * @param annotations 注解数组
     * @return 注解列表字符串，多个注解用逗号分隔
     */
    public static String drawAnnotation(String... annotations) {
        return StringUtils.concat(",", annotations);
    }

    /**
     * 获取类上声明的所有注解
     * <p>
     * 将注解数组转换为字符串数组，每个字符串是注解的类名。
     * </p>
     *
     * @param clazz 要获取注解的类
     * @return 注解类名字符串数组
     */
    public static String[] getAnnotations(Class<?> clazz) {
        return getAnnotations(clazz.getDeclaredAnnotations());
    }

    /**
     * 将注解数组转换为字符串数组
     * <p>
     * 遍历注解数组，提取每个注解的类型名称。
     * </p>
     *
     * @param annotations 注解数组
     * @return 注解类名字符串数组
     */
    public static String[] getAnnotations(Annotation[] annotations) {
        List<String> list = new ArrayList<String>();
        if (annotations != null && annotations.length > 0) {
            for (Annotation annotation : annotations) {
                list.add(StringUtils.classname(annotation.annotationType()));
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * 获取类实现的所有接口
     * <p>
     * 将接口数组转换为类名字符串数组。
     * </p>
     *
     * @param clazz 要获取接口的类
     * @return 接口类名字符串数组
     */
    public static String[] getInterfaces(Class clazz) {
        Class[] interfaces = clazz.getInterfaces();
        return ClassUtils.getClassNameList(interfaces);
    }

    /**
     * 获取类的继承链
     * <p>
     * 从当前类开始，向上遍历所有父类，直到Object类为止。
     * 返回的数组包含从直接父类到Object的所有父类。
     * </p>
     *
     * @param clazz 要获取继承链的类
     * @return 父类类名字符串数组，按继承顺序排列
     */
    public static String[] getSuperClass(Class clazz) {
        List<String> list = new ArrayList<String>();
        Class<?> superClass = clazz.getSuperclass();
        if (null != superClass) {
            list.add(StringUtils.classname(superClass));
            // 继续向上遍历父类
            while (true) {
                superClass = superClass.getSuperclass();
                if (null == superClass) {
                    break;
                }
                list.add(StringUtils.classname(superClass));
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * 获取类的类加载器层次结构
     * <p>
     * 从当前类的类加载器开始，向上遍历所有父类加载器，直到启动类加载器为止。
     * </p>
     *
     * @param clazz 要获取类加载器的类
     * @return 类加载器字符串数组，按层次顺序排列
     */
    public static String[] getClassloader(Class clazz) {
        List<String> list = new ArrayList<String>();
        ClassLoader loader = clazz.getClassLoader();
        if (null != loader) {
            list.add(loader.toString());
            // 继续向上遍历父类加载器
            while (true) {
                loader = loader.getParent();
                if (null == loader) {
                    break;
                }
                list.add(loader.toString());
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * 获取类的所有字段信息
     * <p>
     * 遍历类的所有声明字段，为每个字段创建FieldVO对象。
     * 对于静态字段，还会获取其当前值。
     * </p>
     *
     * @param clazz 要获取字段的类
     * @param expand 对象展开的深度限制
     * @return 字段值对象数组
     */
    public static FieldVO[] getFields(Class clazz, Integer expand) {
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length == 0) {
            return new FieldVO[0];
        }

        List<FieldVO> list = new ArrayList<FieldVO>(fields.length);
        for (Field field : fields) {
            FieldVO fieldVO = new FieldVO();
            // 设置字段的基本信息
            fieldVO.setName(field.getName());
            fieldVO.setType(StringUtils.classname(field.getType()));
            fieldVO.setModifier(StringUtils.modifier(field.getModifiers(), ','));
            fieldVO.setAnnotations(getAnnotations(field.getAnnotations()));

            // 如果是静态字段，获取其值
            if (Modifier.isStatic(field.getModifiers())) {
                fieldVO.setStatic(true);
                fieldVO.setValue(new ObjectVO(getFieldValue(field), expand));
            } else {
                fieldVO.setStatic(false);
            }
            list.add(fieldVO);
        }
        return list.toArray(new FieldVO[0]);
    }

    /**
     * 获取静态字段的值
     * <p>
     * 通过反射获取静态字段的当前值。临时设置字段可访问性，
     * 获取完值后恢复原始的可访问性状态。
     * </p>
     *
     * @param field 要获取值的字段
     * @return 字段的当前值，如果获取失败则返回null
     */
    private static Object getFieldValue(Field field) {
        final boolean isAccessible = field.isAccessible();
        try {
            // 临时设置字段可访问
            field.setAccessible(true);
            Object value = field.get(null);
            return value;
        } catch (IllegalAccessException e) {
            // 访问失败，忽略异常
        } finally {
            // 恢复字段的可访问性
            field.setAccessible(isAccessible);
        }
        return null;
    }

}
