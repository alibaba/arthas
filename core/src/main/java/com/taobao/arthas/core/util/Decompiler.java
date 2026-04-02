package com.taobao.arthas.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns.LineNumberMapping;

import com.taobao.arthas.common.Pair;

/**
 * 反编译器工具类
 * <p>
 * 使用CFR（Class File Reader）反编译器将Java字节码反编译为可读的Java源代码。
 * 支持按方法名反编译、隐藏Unicode字符、打印行号等功能。
 * </p>
 *
 * @author hengyunabc 2018-11-16
 *
 */
public class Decompiler {

    /**
     * 反编译指定类文件中的指定方法
     * <p>
     * 使用默认参数进行反编译，不隐藏Unicode字符，不打印行号
     * </p>
     *
     * @param classFilePath 类文件的路径
     * @param methodName 要反编译的方法名，如果为null或空字符串则反编译整个类
     * @return 反编译后的源代码字符串
     */
    public static String decompile(String classFilePath, String methodName) {
        // 调用重载方法，使用默认参数：不隐藏Unicode，不打印行号
        return decompile(classFilePath, methodName, false);
    }

    /**
     * 反编译指定类文件中的指定方法
     * <p>
     * 支持指定是否隐藏Unicode字符，默认打印行号
     * </p>
     *
     * @param classFilePath 类文件的路径
     * @param methodName 要反编译的方法名，如果为null或空字符串则反编译整个类
     * @param hideUnicode 是否隐藏Unicode字符（例如将中文转换为Unicode转义形式）
     * @return 反编译后的源代码字符串
     */
    public static String decompile(String classFilePath, String methodName, boolean hideUnicode) {
        // 调用重载方法，默认打印行号
        return decompile(classFilePath, methodName, hideUnicode, true);
    }

    /**
     * 反编译指定类文件中的指定方法，并返回反编译结果和行号映射
     * <p>
     * 此方法返回一个Pair对象，包含反编译后的源代码和行号映射关系。
     * 行号映射可以用于将反编译后的代码行映射回源代码的行号。
     * </p>
     *
     * @param classFilePath 类文件的路径
     * @param methodName 要反编译的方法名，如果为null或空字符串则反编译整个类
     * @param hideUnicode 是否隐藏Unicode字符
     * @param printLineNumber 是否在反编译结果中打印行号
     * @return Pair对象，第一个元素是反编译后的源代码，第二个元素是行号映射Map
     *         Map的key是反编译代码的行号，value是源代码的行号
     */
    public static Pair<String, NavigableMap<Integer, Integer>> decompileWithMappings(String classFilePath,
            String methodName, boolean hideUnicode, boolean printLineNumber) {
        // 使用StringBuilder构建反编译结果，初始容量8192字节
        final StringBuilder sb = new StringBuilder(8192);

        // 创建TreeMap存储行号映射关系，自动按key排序
        final NavigableMap<Integer, Integer> lineMapping = new TreeMap<Integer, Integer>();

        // 创建自定义的输出接收器工厂，用于处理CFR反编译器的输出
        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                // 返回支持的接收器类型：字符串、反编译结果、多版本反编译、异常消息、行号映射
                return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.DECOMPILED_MULTIVER,
                        SinkClass.EXCEPTION_MESSAGE, SinkClass.LINE_NUMBER_MAPPING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType sinkType, final SinkClass sinkClass) {
                // 创建并返回一个自定义的Sink来处理输出
                return new Sink<T>() {
                    @Override
                    public void write(T sinkable) {
                        // 跳过进度消息，例如：Analysing type demo.MathGame
                        if (sinkType == SinkType.PROGRESS) {
                            return;
                        }
                        // 处理行号映射信息
                        if (sinkType == SinkType.LINENUMBER) {
                            LineNumberMapping mapping = (LineNumberMapping) sinkable;
                            // 获取类文件行号映射
                            NavigableMap<Integer, Integer> classFileMappings = mapping.getClassFileMappings();
                            // 获取反编译后的行号映射
                            NavigableMap<Integer, Integer> mappings = mapping.getMappings();
                            if (classFileMappings != null && mappings != null) {
                                // 遍历映射关系，建立反编译行号到源代码行号的映射
                                for (Entry<Integer, Integer> entry : mappings.entrySet()) {
                                    // entry.getKey()是字节码行号，通过classFileMappings转换为源代码行号
                                    Integer srcLineNumber = classFileMappings.get(entry.getKey());
                                    // entry.getValue()是反编译后的行号
                                    lineMapping.put(entry.getValue(), srcLineNumber);
                                }
                            }
                            return;
                        }
                        // 将其他输出追加到StringBuilder
                        sb.append(sinkable);
                    }
                };
            }
        };

        // 创建CFR反编译器的配置选项
        HashMap<String, String> options = new HashMap<String, String>();
        /**
         * 配置CFR反编译器选项
         * @see org.benf.cfr.reader.util.MiscConstants.Version.getVersion()
         *      当前CFR版本信息可能不准确，所以禁用显示CFR版本
         */
        options.put("showversion", "false"); // 不显示CFR版本信息
        options.put("hideutf", String.valueOf(hideUnicode)); // 是否隐藏UTF字符（转换为Unicode转义）
        options.put("trackbytecodeloc", "true"); // 跟踪字节码位置，用于生成行号映射
        // 如果指定了方法名，则只反编译该方法
        if (!StringUtils.isBlank(methodName)) {
            options.put("methodname", methodName);
        }

        // 创建CFR驱动器并配置选项和输出接收器
        CfrDriver driver = new CfrDriver.Builder().withOptions(options).withOutputSink(mySink).build();
        // 创建要分析的文件列表
        List<String> toAnalyse = new ArrayList<String>();
        toAnalyse.add(classFilePath);
        // 执行反编译
        driver.analyse(toAnalyse);

        // 获取反编译结果
        String resultCode = sb.toString();
        // 如果需要打印行号且存在行号映射，则添加行号
        if (printLineNumber && !lineMapping.isEmpty()) {
            resultCode = addLineNumber(resultCode, lineMapping);
        }

        // 返回反编译结果和行号映射
        return Pair.make(resultCode, lineMapping);
    }

    /**
     * 反编译指定类文件中的指定方法
     * <p>
     * 支持完整的反编译配置选项，返回反编译后的源代码字符串
     * </p>
     *
     * @param classFilePath 类文件的路径
     * @param methodName 要反编译的方法名，如果为null或空字符串则反编译整个类
     * @param hideUnicode 是否隐藏Unicode字符
     * @param printLineNumber 是否在反编译结果中打印行号
     * @return 反编译后的源代码字符串
     */
    public static String decompile(String classFilePath, String methodName, boolean hideUnicode,
            boolean printLineNumber) {
        // 调用decompileWithMappings方法，只返回反编译结果（不返回行号映射）
        return decompileWithMappings(classFilePath, methodName, hideUnicode, printLineNumber).getFirst();
    }

    /**
     * 为反编译的源代码添加行号注释
     * <p>
     * 在每一行代码前面添加行号注释，格式为 /* 行号 * /。
     * 行号格式会根据最大行号自动调整对齐方式。
     * </p>
     *
     * @param src 反编译后的源代码字符串
     * @param lineMapping 行号映射Map，key是反编译代码行号，value是源代码行号
     * @return 添加了行号注释的源代码字符串
     */
    private static String addLineNumber(String src, Map<Integer, Integer> lineMapping) {
        // 找出最大的源代码行号，用于确定行号格式
        int maxLineNumber = 0;
        for (Integer value : lineMapping.values()) {
            if (value != null && value > maxLineNumber) {
                maxLineNumber = value;
            }
        }

        // 根据最大行号确定行号格式字符串和对应的空格占位符
        String formatStr = "/*%2d*/ "; // 默认格式：2位数字，例如 /*42*/
        String emptyStr = "       "; // 对应的空格占位符，长度与格式化后的行号相同

        StringBuilder sb = new StringBuilder();

        // 将源代码按行分割
        List<String> lines = StringUtils.toLines(src);

        // 根据最大行号调整格式，确保对齐
        if (maxLineNumber >= 1000) {
            // 行号达到4位数，使用4位格式
            formatStr = "/*%4d*/ ";
            emptyStr = "         ";
        } else if (maxLineNumber >= 100) {
            // 行号达到3位数，使用3位格式
            formatStr = "/*%3d*/ ";
            emptyStr = "        ";
        }

        // 遍历每一行代码，添加行号注释
        int index = 0;
        for (String line : lines) {
            // 获取当前行对应的源代码行号（index+1是因为行号从1开始）
            Integer srcLineNumber = lineMapping.get(index + 1);
            if (srcLineNumber != null) {
                // 如果存在源代码行号，格式化添加行号注释
                sb.append(String.format(formatStr, srcLineNumber));
            } else {
                // 如果不存在源代码行号，使用空格占位符对齐
                sb.append(emptyStr);
            }
            // 添加原始代码行并换行
            sb.append(line).append("\n");
            index++;
        }

        return sb.toString();
    }

}
