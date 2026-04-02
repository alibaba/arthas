package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * Java方法信息控件
 * 用于展示Java方法的详细信息，包括声明类、方法名、修饰符、注解、参数、返回值和异常等
 * Created by vlinux on 15/5/9.
 */
public class MethodInfoView implements View {

    // 要展示的Method对象
    private final Method method;
    // 表格的宽度限制
    private final int width;

    /**
     * 构造函数
     * @param method 要展示的Java方法对象
     * @param width 表格的显示宽度
     */
    public MethodInfoView(Method method, int width) {
        this.method = method;
        this.width = width;
    }

    /**
     * 绘制方法信息的表格视图
     * @return 格式化后的方法信息表格字符串
     */
    @Override
    public String draw() {
        // 创建一个两列的表格：第一列是属性名（右对齐），第二列是属性值（左对齐）
        return new TableView(new TableView.ColumnDefine[]{
                // 第一列：属性名列，宽度为"declaring-class"的长度，右对齐
                new TableView.ColumnDefine("declaring-class".length(), false, TableView.Align.RIGHT),
                // 第二列：属性值列，宽度为总宽度减去第一列宽度和其他字符（分隔符等）的宽度
                // (列数-1) * 3 + 4 = 7，这些是表格边框和分隔符占用的字符数
                new TableView.ColumnDefine(width - "declaring-class".length() - 7, false, TableView.Align.LEFT)
        })
                // 添加方法的各项信息行
                .addRow("declaring-class", method.getDeclaringClass().getName())  // 声明类
                .addRow("method-name", method.getName())                            // 方法名
                .addRow("modifier", StringUtils.modifier(method.getModifiers(), ',')) // 修饰符（public、private等）
                .addRow("annotation", drawAnnotation())                              // 注解
                .addRow("parameters", drawParameters())                              // 参数列表
                .addRow("return", drawReturn())                                      // 返回值类型
                .addRow("exceptions", drawExceptions())                              // 声明的异常
                .padding(1)              // 设置内边距为1
                .hasBorder(true)          // 显示表格边框
                .draw();                  // 绘制表格
    }

    /**
     * 绘制方法的注解信息
     * 将方法上的所有注解类型名称用逗号连接成一个字符串
     * @return 注解类型名称的字符串，无注解时返回空字符串
     */
    private String drawAnnotation() {

        final StringBuilder annotationSB = new StringBuilder();
        // 获取方法上声明的所有注解（不包括继承的注解）
        final Annotation[] annotationArray = method.getDeclaredAnnotations();

        if (annotationArray.length > 0) {
            // 遍历所有注解，将注解类型名称添加到StringBuilder中
            for (Annotation annotation : annotationArray) {
                annotationSB.append(StringUtils.classname(annotation.annotationType())).append(",");
            }
            // 删除最后一个逗号
            if (annotationSB.length() > 0) {
                annotationSB.deleteCharAt(annotationSB.length() - 1);
            }
        } else {
            // 没有注解时返回空字符串
            annotationSB.append(Constants.EMPTY_STRING);
        }

        return annotationSB.toString();
    }

    /**
     * 绘制方法的参数列表
     * 将每个参数的类型名称占一行显示
     * @return 参数类型名称的字符串，每个类型一行
     */
    private String drawParameters() {
        final StringBuilder paramsSB = new StringBuilder();
        // 获取方法的所有参数类型
        final Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0) {
            // 遍历所有参数类型，每个类型占一行
            for (Class<?> clazz : paramTypes) {
                paramsSB.append(StringUtils.classname(clazz)).append("\n");
            }
        }
        return paramsSB.toString();
    }

    /**
     * 绘制方法的返回值类型
     * @return 返回值类型的类名称
     */
    private String drawReturn() {
        final StringBuilder returnSB = new StringBuilder();
        // 获取方法的返回值类型
        final Class<?> returnTypeClass = method.getReturnType();
        returnSB.append(StringUtils.classname(returnTypeClass)).append("\n");
        return returnSB.toString();
    }

    /**
     * 绘制方法声明的异常列表
     * 将方法throws子句中声明的所有异常类型显示出来
     * @return 异常类型名称的字符串，每个类型一行
     */
    private String drawExceptions() {
        final StringBuilder exceptionSB = new StringBuilder();
        // 获取方法声明的所有异常类型
        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        if (exceptionTypes.length > 0) {
            // 遍历所有异常类型，每个类型占一行
            for (Class<?> clazz : exceptionTypes) {
                exceptionSB.append(StringUtils.classname(clazz)).append("\n");
            }
        }
        return exceptionSB.toString();
    }

}
