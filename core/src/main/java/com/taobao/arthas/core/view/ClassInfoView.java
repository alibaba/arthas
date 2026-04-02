package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.CodeSource;

/**
 * Java类信息视图控件
 * 用于展示Java类的详细信息，包括类的基本信息、注解、接口、父类、类加载器等
 * Created by vlinux on 15/5/7.
 */
public class ClassInfoView implements View {

    // 要展示的Class对象
    private final Class<?> clazz;
    // 是否打印字段信息
    private final boolean isPrintField;
    // 输出宽度（用于格式化）
    private final int width;

    /**
     * 构造函数
     * @param clazz 要展示的Java类
     * @param isPrintField 是否打印字段信息
     * @param width 输出宽度
     */
    public ClassInfoView(Class<?> clazz, boolean isPrintField, int width) {
        this.clazz = clazz;
        this.isPrintField = isPrintField;
        this.width = width;
    }

    @Override
    public String draw() {
        // 绘制类信息视图
        return drawClassInfo();
    }

    /**
     * 获取代码源位置
     * @param cs CodeSource对象
     * @return 代码源文件路径，如果无法获取则返回空字符串
     */
    private String getCodeSource(final CodeSource cs) {
        // 检查CodeSource及其位置是否为null
        if (null == cs
                || null == cs.getLocation()
                || null == cs.getLocation().getFile()) {
            return Constants.EMPTY_STRING;
        }

        // 返回代码源位置的文件路径
        return cs.getLocation().getFile();
    }

    /**
     * 绘制类信息
     * @return 格式化的类信息字符串
     */
    private String drawClassInfo() {
        // 获取类的代码源（包含类的位置信息）
        final CodeSource cs = clazz.getProtectionDomain().getCodeSource();

        // 创建表格视图，定义两列：标签列和值列
        // 第一列：右对齐，宽度为"isAnonymousClass"的长度
        // 第二列：左对齐，宽度为总宽度减去第一列宽度、边框和分隔符的宽度
        // (列数-1) * 3 + 4 = 7 是边框和分隔符占用的宽度
        final TableView view = new TableView(new TableView.ColumnDefine[]{
                new TableView.ColumnDefine("isAnonymousClass".length(), false, TableView.Align.RIGHT),
                new TableView.ColumnDefine(width - "isAnonymousClass".length() - 7, false, TableView.Align.LEFT)
        })
                // 添加类的基本信息行
                .addRow("class-info", StringUtils.classname(clazz))
                .addRow("code-source", getCodeSource(cs))
                .addRow("name", StringUtils.classname(clazz))
                // 添加类的类型判断信息
                .addRow("isInterface", clazz.isInterface())
                .addRow("isAnnotation", clazz.isAnnotation())
                .addRow("isEnum", clazz.isEnum())
                .addRow("isAnonymousClass", clazz.isAnonymousClass())
                .addRow("isArray", clazz.isArray())
                .addRow("isLocalClass", clazz.isLocalClass())
                .addRow("isMemberClass", clazz.isMemberClass())
                .addRow("isPrimitive", clazz.isPrimitive())
                .addRow("isSynthetic", clazz.isSynthetic())
                // 添加类的名称和修饰符信息
                .addRow("simple-name", clazz.getSimpleName())
                .addRow("modifier", StringUtils.modifier(clazz.getModifiers(), ','))
                // 添加注解、接口、父类和类加载器信息
                .addRow("annotation", drawAnnotation())
                .addRow("interfaces", drawInterface())
                .addRow("super-class", drawSuperClass())
                .addRow("class-loader", drawClassLoader());

        // 如果需要打印字段信息，则添加字段行
        if (isPrintField) {
            view.addRow("fields", drawField());
        }

        // 返回带边框、内边距为1的表格
        return view.hasBorder(true).padding(1).draw();
    }


    /**
     * 绘制字段信息
     * 遍历类的所有声明字段，为每个字段创建一个键值视图
     * @return 格式化的字段信息字符串
     */
    private String drawField() {

        final StringBuilder fieldSB = new StringBuilder();

        // 获取类声明的所有字段（包括私有字段）
        final Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {

            // 遍历每个字段
            for (Field field : fields) {

                // 为每个字段创建键值视图，显示修饰符、类型和名称
                final KVView kvView = new KVView(new TableView.ColumnDefine(TableView.Align.RIGHT), new TableView.ColumnDefine(50, false, TableView.Align.LEFT))
                        .add("modifier", StringUtils.modifier(field.getModifiers(), ','))
                        .add("type", StringUtils.classname(field.getType()))
                        .add("name", field.getName());


                // 处理字段的注解
                final StringBuilder annotationSB = new StringBuilder();
                final Annotation[] annotationArray = field.getAnnotations();
                if (null != annotationArray && annotationArray.length > 0) {
                    // 将所有注解类型名称用逗号连接
                    for (Annotation annotation : annotationArray) {
                        annotationSB.append(StringUtils.classname(annotation.annotationType())).append(",");
                    }
                    // 移除最后一个逗号
                    if (annotationSB.length() > 0) {
                        annotationSB.deleteCharAt(annotationSB.length() - 1);
                    }
                    kvView.add("annotation", annotationSB);
                }


                // 如果是静态字段，尝试获取其值
                if (Modifier.isStatic(field.getModifiers())) {
                    // 保存原始的可访问性状态
                    final boolean isAccessible = field.isAccessible();
                    try {
                        // 设置为可访问以读取私有静态字段的值
                        field.setAccessible(true);
                        // 获取静态字段的值（传入null因为静态字段不需要对象实例）
                        kvView.add("value", StringUtils.objectToString(field.get(null)));
                    } catch (IllegalAccessException e) {
                        // 如果无法访问，忽略异常
                        //
                    } finally {
                        // 恢复原始的可访问性状态
                        field.setAccessible(isAccessible);
                    }
                }

                // 将字段信息追加到结果字符串
                fieldSB.append(kvView.draw()).append("\n");

            }

        }

        return fieldSB.toString();
    }

    /**
     * 绘制注解信息
     * @return 类的所有注解类型名称，用逗号分隔
     */
    private String drawAnnotation() {
        final StringBuilder annotationSB = new StringBuilder();
        // 获取类声明的所有注解
        final Annotation[] annotationArray = clazz.getDeclaredAnnotations();

        if (annotationArray.length > 0) {
            // 将每个注解的类型名称用逗号连接
            for (Annotation annotation : annotationArray) {
                annotationSB.append(StringUtils.classname(annotation.annotationType())).append(",");
            }
            // 移除最后一个逗号
            if (annotationSB.length() > 0) {
                annotationSB.deleteCharAt(annotationSB.length() - 1);
            }
        } else {
            // 如果没有注解，返回空字符串
            annotationSB.append(Constants.EMPTY_STRING);
        }

        return annotationSB.toString();
    }

    /**
     * 绘制接口信息
     * @return 类实现的所有接口名称，用逗号分隔
     */
    private String drawInterface() {
        final StringBuilder interfaceSB = new StringBuilder();
        // 获取类实现的所有接口
        final Class<?>[] interfaceArray = clazz.getInterfaces();
        if (interfaceArray.length == 0) {
            // 如果没有实现接口，返回空字符串
            interfaceSB.append(Constants.EMPTY_STRING);
        } else {
            // 将每个接口的名称用逗号连接
            for (Class<?> i : interfaceArray) {
                interfaceSB.append(i.getName()).append(",");
            }
            // 移除最后一个逗号
            if (interfaceSB.length() > 0) {
                interfaceSB.deleteCharAt(interfaceSB.length() - 1);
            }
        }
        return interfaceSB.toString();
    }

    /**
     * 绘制父类继承层次结构
     * 使用阶梯视图显示类的继承链
     * @return 格式化的父类继承层次结构
     */
    private String drawSuperClass() {
        // 创建阶梯视图，用于显示继承层次
        final LadderView ladderView = new LadderView();
        // 获取直接父类
        Class<?> superClass = clazz.getSuperclass();
        if (null != superClass) {
            // 添加父类到视图
            ladderView.addItem(StringUtils.classname(superClass));
            // 继续向上追溯父类的父类
            while (true) {
                superClass = superClass.getSuperclass();
                if (null == superClass) {
                    // 到达继承链顶端（Object类之前）
                    break;
                }
                ladderView.addItem(StringUtils.classname(superClass));
            }//while
        }
        return ladderView.draw();
    }


    /**
     * 绘制类加载器层次结构
     * 使用阶梯视图显示类加载器的父加载器链
     * @return 格式化的类加载器层次结构
     */
    private String drawClassLoader() {
        // 创建阶梯视图，用于显示类加载器层次
        final LadderView ladderView = new LadderView();
        // 获取加载当前类的类加载器
        ClassLoader loader = clazz.getClassLoader();
        if (null != loader) {
            // 添加类加载器到视图
            ladderView.addItem(loader.toString());
            // 继续向上追溯父类加载器
            while (true) {
                loader = loader.getParent();
                if (null == loader) {
                    // 到达类加载器层次顶端（启动类加载器）
                    break;
                }
                ladderView.addItem(loader.toString());
            }
        }
        return ladderView.draw();
    }

}
