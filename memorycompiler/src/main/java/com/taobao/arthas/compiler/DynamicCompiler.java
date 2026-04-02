package com.taobao.arthas.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * 动态编译器
 *
 * <p>用于在运行时动态编译Java源代码。该类封装了Java Compiler API，
 * 提供了便捷的接口来编译源代码并获取编译后的类或字节码。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>动态添加Java源代码</li>
 *   <li>编译源代码并生成Class对象</li>
 *   <li>编译源代码并生成字节数组</li>
 *   <li>收集编译错误和警告信息</li>
 *   <li>提供对动态类加载器的访问</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * DynamicCompiler compiler = new DynamicCompiler(getClass().getClassLoader());
 * compiler.addSource("com.example.MyClass", sourceCode);
 * Map<String, Class<?>> classes = compiler.build();
 * }</pre>
 *
 * @author arthas
 * @since 2017-2018
 */
public class DynamicCompiler {

    /**
     * 系统Java编译器
     * 通过ToolProvider获取，用于编译Java源代码
     */
    private final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

    /**
     * 标准Java文件管理器
     * 用于管理编译过程中的文件操作
     */
    private final StandardJavaFileManager standardFileManager;

    /**
     * 编译选项列表
     * 存储传递给编译器的各种选项，如警告级别、调试信息等
     */
    private final List<String> options = new ArrayList<String>();

    /**
     * 动态类加载器
     * 用于加载编译后生成的类
     */
    private final DynamicClassLoader dynamicClassLoader;

    /**
     * 编译单元集合
     * 存储待编译的Java源代码文件对象
     */
    private final Collection<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();

    /**
     * 编译错误信息列表
     * 存储编译过程中产生的所有错误诊断信息
     */
    private final List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<Diagnostic<? extends JavaFileObject>>();

    /**
     * 编译警告信息列表
     * 存储编译过程中产生的所有警告诊断信息
     */
    private final List<Diagnostic<? extends JavaFileObject>> warnings = new ArrayList<Diagnostic<? extends JavaFileObject>>();

    /**
     * 构造函数
     *
     * @param classLoader 父类加载器，用于动态类加载器的双亲委派
     * @throws IllegalStateException 如果无法获取Java编译器（通常是因为运行在JRE而非JDK环境）
     */
    public DynamicCompiler(ClassLoader classLoader) {
        // 检查是否成功获取了Java编译器
        if (javaCompiler == null) {
            throw new IllegalStateException(
                            "Can not load JavaCompiler from javax.tools.ToolProvider#getSystemJavaCompiler(),"
                                            + " please confirm the application running in JDK not JRE.");
        }

        // 初始化标准文件管理器
        // 参数：diagnosticListener(诊断监听器), locale(区域设置), charset(字符集)
        standardFileManager = javaCompiler.getStandardFileManager(null, null, null);

        // 添加编译选项
        options.add("-Xlint:unchecked");  // 启用未检查操作的警告
        options.add("-g");                  // 生成调试信息

        // 创建动态类加载器
        dynamicClassLoader = new DynamicClassLoader(classLoader);
    }

    /**
     * 添加Java源代码
     *
     * <p>创建一个字符串源对象并添加到待编译列表中。</p>
     *
     * @param className 类的全限定名
     * @param source Java源代码内容
     */
    public void addSource(String className, String source) {
        // 将类名和源代码封装为StringSource对象并添加到编译单元
        addSource(new StringSource(className, source));
    }

    /**
     * 添加Java文件对象
     *
     * <p>直接将一个Java文件对象添加到待编译列表中。</p>
     *
     * @param javaFileObject Java文件对象
     */
    public void addSource(JavaFileObject javaFileObject) {
        // 将文件对象添加到编译单元集合
        compilationUnits.add(javaFileObject);
    }

    /**
     * 编译源代码并返回Class对象
     *
     * <p>执行编译过程，将所有已添加的源代码编译为Class对象。
     * 如果编译失败，将抛出包含错误信息的异常。</p>
     *
     * @return 类名到Class对象的映射表
     * @throws DynamicCompilerException 如果编译过程中发生错误
     */
    public Map<String, Class<?>> build() {

        // 清空之前的错误和警告信息
        errors.clear();
        warnings.clear();

        // 创建自定义的文件管理器，包装标准文件管理器并使用动态类加载器
        JavaFileManager fileManager = new DynamicJavaFileManager(standardFileManager, dynamicClassLoader);

        // 创建诊断收集器，用于收集编译过程中的诊断信息
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();

        // 创建编译任务
        // 参数：out(输出流), fileManager(文件管理器), diagnosticListener(诊断监听器),
        //      options(编译选项), classes(类名), compilationUnits(编译单元)
        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, collector, options, null,
                        compilationUnits);

        try {

            // 如果有待编译的源代码
            if (!compilationUnits.isEmpty()) {
                // 执行编译任务
                boolean result = task.call();

                // 如果编译失败或有诊断信息产生
                if (!result || collector.getDiagnostics().size() > 0) {

                    // 遍历所有诊断信息
                    for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
                        // 根据诊断信息的类型进行分类
                        switch (diagnostic.getKind()) {
                        case NOTE:              // 提示信息
                        case MANDATORY_WARNING: // 强制警告
                        case WARNING:           // 警告
                            // 添加到警告列表
                            warnings.add(diagnostic);
                            break;
                        case OTHER:             // 其他信息
                        case ERROR:             // 错误
                        default:
                            // 添加到错误列表
                            errors.add(diagnostic);
                            break;
                        }

                    }

                    // 如果有编译错误，抛出异常
                    if (!errors.isEmpty()) {
                        throw new DynamicCompilerException("Compilation Error", errors);
                    }
                }
            }

            // 返回动态类加载器加载的所有类
            return dynamicClassLoader.getClasses();
        } catch (Throwable e) {
            // 捕获所有异常并包装为DynamicCompilerException
            throw new DynamicCompilerException(e, errors);
        } finally {
            // 清理编译单元，为下一次编译做准备
            compilationUnits.clear();

        }

    }

    /**
     * 编译源代码并返回字节数组
     *
     * <p>执行编译过程，将所有已添加的源代码编译为字节数组。
     * 这些字节数组是编译后的类的原始字节码。</p>
     *
     * @return 类名到字节数组的映射表
     * @throws DynamicCompilerException 如果编译过程中发生错误
     */
    public Map<String, byte[]> buildByteCodes() {

        // 清空之前的错误和警告信息
        errors.clear();
        warnings.clear();

        // 创建自定义的文件管理器
        JavaFileManager fileManager = new DynamicJavaFileManager(standardFileManager, dynamicClassLoader);

        // 创建诊断收集器
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();

        // 创建编译任务
        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, collector, options, null,
                        compilationUnits);

        try {

            // 如果有待编译的源代码
            if (!compilationUnits.isEmpty()) {
                // 执行编译任务
                boolean result = task.call();

                // 如果编译失败或有诊断信息产生
                if (!result || collector.getDiagnostics().size() > 0) {

                    // 遍历所有诊断信息
                    for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
                        // 根据诊断信息的类型进行分类
                        switch (diagnostic.getKind()) {
                        case NOTE:              // 提示信息
                        case MANDATORY_WARNING: // 强制警告
                        case WARNING:           // 警告
                            // 添加到警告列表
                            warnings.add(diagnostic);
                            break;
                        case OTHER:             // 其他信息
                        case ERROR:             // 错误
                        default:
                            // 添加到错误列表
                            errors.add(diagnostic);
                            break;
                        }

                    }

                    // 如果有编译错误，抛出异常
                    if (!errors.isEmpty()) {
                        throw new DynamicCompilerException("Compilation Error", errors);
                    }
                }
            }

            // 返回动态类加载器中的所有字节码
            return dynamicClassLoader.getByteCodes();
        } catch (ClassFormatError e) {
            // 捕获类格式错误（通常是字节码格式不正确）
            throw new DynamicCompilerException(e, errors);
        } finally {
            // 清理编译单元，为下一次编译做准备
            compilationUnits.clear();

        }

    }

    /**
     * 将诊断信息转换为字符串列表
     *
     * <p>将诊断对象列表转换为可读的字符串格式，
     * 包含行号和错误消息。</p>
     *
     * @param diagnostics 诊断信息列表
     * @return 格式化后的诊断消息列表
     */
    private List<String> diagnosticToString(List<Diagnostic<? extends JavaFileObject>> diagnostics) {

        // 创建结果列表
        List<String> diagnosticMessages = new ArrayList<String>();

        // 遍历所有诊断信息
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            // 格式化诊断消息：行号 + 消息内容
            diagnosticMessages.add(
                            "line: " + diagnostic.getLineNumber() + ", message: " + diagnostic.getMessage(Locale.US));
        }

        return diagnosticMessages;

    }

    /**
     * 获取编译错误信息
     *
     * @return 格式化后的错误消息列表
     */
    public List<String> getErrors() {
        return diagnosticToString(errors);
    }

    /**
     * 获取编译警告信息
     *
     * @return 格式化后的警告消息列表
     */
    public List<String> getWarnings() {
        return diagnosticToString(warnings);
    }

    /**
     * 获取动态类加载器
     *
     * <p>返回用于加载编译后类的类加载器。
     * 可以通过这个类加载器来访问编译后的类。</p>
     *
     * @return 动态类加载器
     */
    public ClassLoader getClassLoader() {
        return dynamicClassLoader;
    }
}
