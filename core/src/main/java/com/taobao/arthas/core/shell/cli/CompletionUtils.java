package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import io.termd.core.util.Helper;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 自动补全工具类
 *
 * 该类提供了一系列静态方法用于处理命令行的自动补全功能，包括：
 * - 查找最长公共前缀
 * - 根据类定义进行补全
 * - 在给定集合中查找匹配项进行补全
 * - 文件路径补全
 * - 类名补全
 * - 方法名补全
 * - 选项补全等
 *
 * @author beiwei30 on 09/11/2016.
 */
public class CompletionUtils {

    /**
     * 查找给定字符串集合的最长公共前缀
     *
     * 该方法将字符串转换为码点数组，然后查找最长公共前缀
     *
     * @param values 字符串集合
     * @return 最长公共前缀
     */
    public static String findLongestCommonPrefix(Collection<String> values) {
        // 创建一个整数数组列表，用于存储每个字符串的码点数组
        List<int[]> entries = new LinkedList<int[]>();
        for (String value : values) {
            // 将字符串转换为码点数组并添加到列表中
            int[] entry = Helper.toCodePoints(value);
            entries.add(entry);
        }
        // 使用termd库的Completion类查找最长公共前缀，并将其转换回字符串
        return Helper.fromCodePoints(io.termd.core.readline.Completion.findLongestCommonPrefix(entries));
    }

    /**
     * 根据给定的类定义进行自动补全
     *
     * 该方法会根据最后一个token的类型决定补全行为：
     * - 如果最后一个token为空或为null，则显示使用说明
     * - 如果以"--"开头，则补全长选项
     * - 如果以"-"开头，则补全短选项
     * - 其他情况不进行补全
     *
     * @param completion 补全对象
     * @param clazz 用于获取CLI定义的类
     */
    public static void complete(Completion completion, Class<?> clazz) {
        // 获取命令行token列表
        List<CliToken> tokens = completion.lineTokens();
        // 获取最后一个token
        CliToken lastToken = tokens.get(tokens.size() - 1);
        // 根据类定义创建CLI对象
        CLI cli = CLIConfigurator.define(clazz);
        // 获取所有选项
        List<com.taobao.middleware.cli.Option> options = cli.getOptions();
        // 如果最后一个token为null或为空，显示使用说明
        if (lastToken == null || lastToken.isBlank()) {
            completeUsage(completion, cli);
        } else if (lastToken.value().startsWith("--")) {
            // 如果以"--"开头，补全长选项
            completeLongOption(completion, lastToken, options);
        } else if (lastToken.value().startsWith("-")) {
            // 如果以"-"开头，补全短选项
            completeShortOption(completion, lastToken, options);
        } else {
            // 其他情况不进行补全，返回空列表
            completion.complete(Collections.<String>emptyList());
        }
    }

    /**
     * 从给定的查询数组中查询匹配的对象，并进行自动补全
     *
     * 该方法在searchScope集合中查找以lastToken开头的所有字符串，并根据匹配数量进行补全：
     * - 如果只有一个匹配项，直接补全该项
     * - 如果有多个匹配项，显示所有匹配项供用户选择
     *
     * @param completion 补全对象
     * @param searchScope 搜索范围，即待补全的字符串集合
     * @return 是否成功完成补全
     */
    public static boolean complete(Completion completion, Collection<String> searchScope) {
        // 获取命令行token列表
        List<CliToken> tokens = completion.lineTokens();
        // 获取最后一个token的值
        String lastToken = tokens.get(tokens.size() - 1).value();
        // 创建候选列表
        List<String> candidates = new ArrayList<String>();

        // 如果最后一个token为空，则设为空字符串
        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        // 在搜索范围内查找以lastToken开头的所有字符串
        for (String name: searchScope) {
            if (name.startsWith(lastToken)) {
                candidates.add(name);
            }
        }
        // 如果只有一个候选，直接补全
        if (candidates.size() == 1) {
            completion.complete(candidates.get(0).substring(lastToken.length()), true);
            return true;
        } else {
            // 如果有多个候选，显示所有候选
            completion.complete(candidates);
            return true;
        }
    }

    /**
     * 判断给定的token是否表示目录的结束
     *
     * @param token 待判断的token
     * @return 如果token以文件分隔符结尾，返回true；否则返回false
     */
    private static boolean isEndOfDirectory(String token) {
        return !StringUtils.isBlank(token) && (token.endsWith(File.separator) || token.endsWith("/"));
    }

    /**
     * 完成文件路径的自动补全
     *
     * 该方法根据当前的token补全文件路径。它会：
     * 1. 解析当前路径和部分名称
     * 2. 列出目录中匹配的文件和子目录
     * 3. 对子目录添加"/"后缀
     * 4. 根据匹配数量进行补全或显示候选列表
     *
     * 返回true表示已经完成completion，返回false表示没有，调用者需要另外完成补全
     *
     * @param completion 补全对象
     * @return 是否成功完成补全
     */
    public static boolean completeFilePath(Completion completion) {
        // 获取命令行token列表
        List<CliToken> tokens = completion.lineTokens();
        // 获取最后一个token的值
        String token = tokens.get(tokens.size() - 1).value();

        // 如果token以"-"开头或为空，不进行补全
        if (token.startsWith("-") || StringUtils.isBlank(token)) {
            return false;
        }

        // 初始化目录和部分名称
        File dir = null;
        String partName = "";
        // 如果token为空，使用当前目录
        if (StringUtils.isBlank(token)) {
            dir = new File("").getAbsoluteFile();
            token = "";
        } else if (isEndOfDirectory(token)) {
            // 如果token以文件分隔符结尾，使用该路径作为目录
            dir = new File(token);
        } else {
            // 获取token的父目录
            File parent = new File(token).getAbsoluteFile().getParentFile();
            if (parent != null && parent.exists()) {
                dir = parent;
                partName = new File(token).getName();
            }
        }

        // 创建token对应的文件对象
        File tokenFile = new File(token);

        // 获取token文件名
        String tokenFileName = null;
        if (isEndOfDirectory(token)) {
            tokenFileName = "";
        } else {
            tokenFileName = tokenFile.getName();
        }

        // 如果目录为null，返回false
        if (dir == null) {
            return false;
        }

        // 列出目录中的所有文件
        File[] listFiles = dir.listFiles();

        // 创建匹配的文件名列表
        ArrayList<String> names = new ArrayList<>();
        if (listFiles != null) {
            for (File child : listFiles) {
                // 查找以partName开头的文件或目录
                if (child.getName().startsWith(partName)) {
                    if (child.isDirectory()) {
                        // 对目录添加"/"后缀
                        names.add(child.getName() + "/");
                    } else {
                        names.add(child.getName());
                    }
                }
            }
        }

        // 如果只有一个匹配项且是目录，直接补全
        if (names.size() == 1 && isEndOfDirectory(names.get(0))) {
            String name = names.get(0);
            // 这个函数补全后不会有空格，并且只能传入要补全的内容
            completion.complete(name.substring(tokenFileName.length()), false);
            return true;
        }

        // 计算前缀路径
        String prefix = null;
        if (isEndOfDirectory(token)) {
            prefix = token;
        } else {
            prefix = token.substring(0, token.length() - new File(token).getName().length());
        }

        // 创建带前缀的文件名列表
        ArrayList<String> namesWithPrefix = new ArrayList<>();
        for (String name : names) {
            namesWithPrefix.add(prefix + name);
        }
        // 这个函数需要保留前缀
        CompletionUtils.complete(completion, namesWithPrefix);
        return true;
    }

    /**
     * 完成类名的自动补全
     *
     * 该方法利用Java Instrumentation API获取所有已加载的类，
     * 并根据用户输入的前缀进行补全。如果补全结果是唯一的包名，则继续补全；
     * 否则显示所有匹配的类或包。
     *
     * @param completion 补全对象
     * @return 是否成功完成补全
     */
    public static boolean completeClassName(Completion completion) {
        // 获取命令行token列表
        List<CliToken> tokens = completion.lineTokens();
        // 获取最后一个token的值
        String lastToken = tokens.get(tokens.size() - 1).value();

        // 如果最后一个token为空，设为空字符串
        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        // 如果token以"-"开头，不进行补全
        if (lastToken.startsWith("-")) {
            return false;
        }

        // 获取Instrumentation实例
        Instrumentation instrumentation = completion.session().getInstrumentation();

        // 获取所有已加载的类
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();

        // 创建结果集，用于存储匹配的类名或包名
        Set<String> result = new HashSet<String>();
        // 遍历所有已加载的类
        for(Class<?> clazz : allLoadedClasses) {
            String name = clazz.getName();
            // 跳过数组类型
            if (name.startsWith("[")) {
                continue;
            }
            // 查找以lastToken开头的类名
            if(name.startsWith(lastToken)) {
                // 查找下一个包分隔符的位置
                int index = name.indexOf('.', lastToken.length());

                if(index > 0) {
                    // 如果找到包分隔符，添加包名
                    result.add(name.substring(0, index + 1));
                }else {
                    // 否则添加完整类名
                    result.add(name);
                }

            }
        }

        // 如果只有一个结果且以"."结尾，说明是包名，继续补全
        if(result.size() == 1 && result.iterator().next().endsWith(".")) {
            completion.complete(result.iterator().next().substring(lastToken.length()), false);
        }else {
            // 否则显示所有匹配结果
            CompletionUtils.complete(completion, result);
        }
        return true;
    }

    /**
     * 完成方法名的自动补全
     *
     * 该方法首先查找用户输入的类名对应的类，然后列出该类的所有方法名，
     * 根据用户输入的前缀进行补全。
     *
     * @param completion 补全对象
     * @return 是否成功完成补全
     */
    public static boolean completeMethodName(Completion completion) {
        // 获取命令行token列表
        List<CliToken> tokens = completion.lineTokens();
        // 获取最后一个token的值
        String lastToken = completion.lineTokens().get(tokens.size() - 1).value();

        // 如果最后一个token为空，设为空字符串
        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        // 获取类名
        String className;
        if (StringUtils.isBlank(lastToken)) {
            // 如果lastToken为空，类名在倒数第二个位置
            // tokens = { " ", "CLASS_NAME", " "}
            className = tokens.get(tokens.size() - 2).value();
        } else {
            // 否则类名在倒数第三个位置
            // tokens = { " ", "CLASS_NAME", " ", "PARTIAL_METHOD_NAME"}
            className = tokens.get(tokens.size() - 3).value();
        }

        // 搜索类
        Set<Class<?>> results = SearchUtils.searchClassOnly(completion.session().getInstrumentation(), className, 2);
        // 如果找不到类或找到多个类，返回空列表
        if (results.size() != 1) {
            // no class found or multiple class found
            completion.complete(Collections.<String>emptyList());
            return true;
        }

        // 获取唯一的类
        Class<?> clazz = results.iterator().next();

        // 创建方法名列表
        List<String> res = new ArrayList<String>();

        // 遍历类的所有声明方法
        for (Method method : clazz.getDeclaredMethods()) {
            if (StringUtils.isBlank(lastToken)) {
                // 如果lastToken为空，添加所有方法名
                res.add(method.getName());
            } else if (method.getName().startsWith(lastToken)) {
                // 否则只添加以lastToken开头的方法名
                res.add(method.getName());
            }
        }
        // 添加构造函数标记
        res.add("<init>");

        // 如果只有一个匹配，直接补全
        if (res.size() == 1) {
            completion.complete(res.get(0).substring(lastToken.length()), true);
            return true;
        } else {
            // 否则显示所有匹配
            CompletionUtils.complete(completion, res);
            return true;
        }
    }

    /**
     * 推断当前输入到第几个参数
     *
     * 该方法通过统计命令行中的有效token数量来判断用户当前在输入第几个参数。
     * 忽略空token和以"-"开头的选项token。
     *
     * @param completion 补全对象
     * @return 参数索引，从1开始；如果当前正在输入选项，返回-1
     */
    public static int detectArgumentIndex(Completion completion) {
        // 获取命令行token列表
        List<CliToken> tokens = completion.lineTokens();
        // 获取最后一个token
        CliToken lastToken = tokens.get(tokens.size() - 1);

        // 如果最后一个token以"-"或"--"开头，说明正在输入选项，返回-1
        if (lastToken.value().startsWith("-") || lastToken.value().startsWith("--")) {
            return -1;
        }

        // 如果只有一个token且为空，说明正在输入第一个参数
        if (StringUtils.isBlank((lastToken.value())) && tokens.size() == 1) {
            return 1;
        }

        // 统计有效的token数量
        int tokenCount = 0;

        for (CliToken token : tokens) {
            // 跳过空token和以"-"开头的选项token
            if (StringUtils.isBlank(token.value()) || token.value().startsWith("-") || token.value().startsWith("--")) {
                // filter irrelevant tokens
                continue;
            }
            tokenCount++;
        }

        // 如果最后一个token为空且不是第一个token，说明用户正在输入下一个参数
        if (StringUtils.isBlank((lastToken.value())) && tokens.size() != 1) {
            tokenCount++;
        }
        return tokenCount;
    }

    /**
     * 补全短选项
     *
     * 短选项是以"-"开头的单字符选项，如"-h"、"-v"等
     *
     * @param completion 补全对象
     * @param lastToken 最后一个token
     * @param options 所有选项列表
     */
    public static void completeShortOption(Completion completion, CliToken lastToken, List<Option> options) {
        // 获取前缀（去掉开头的"-"）
        String prefix = lastToken.value().substring(1);
        // 创建候选列表
        List<String> candidates = new ArrayList<String>();
        // 遍历所有选项，查找短名称与前缀匹配的选项
        for (Option option : options) {
            if (option.getShortName().startsWith(prefix)) {
                candidates.add(option.getShortName());
            }
        }
        // 进行补全
        complete(completion, prefix, candidates);
    }

    /**
     * 补全长选项
     *
     * 长选项是以"--"开头的多字符选项，如"--help"、"--verbose"等
     *
     * @param completion 补全对象
     * @param lastToken 最后一个token
     * @param options 所有选项列表
     */
    public static void completeLongOption(Completion completion, CliToken lastToken, List<Option> options) {
        // 获取前缀（去掉开头的"--"）
        String prefix = lastToken.value().substring(2);
        // 创建候选列表
        List<String> candidates = new ArrayList<String>();
        // 遍历所有选项，查找长名称与前缀匹配的选项
        for (Option option : options) {
            if (option.getLongName().startsWith(prefix)) {
                candidates.add(option.getLongName());
            }
        }
        // 进行补全
        complete(completion, prefix, candidates);
    }

    /**
     * 显示使用说明
     *
     * 该方法生成CLI的使用说明，并将其作为补全结果显示给用户
     *
     * @param completion 补全对象
     * @param cli CLI对象
     */
    public static void completeUsage(Completion completion, CLI cli) {
        // 获取终端对象
        Tty tty = completion.session().get(Session.TTY);
        // 生成带样式使用说明，根据终端宽度进行格式化
        String usage = StyledUsageFormatter.styledUsage(cli, tty.width());
        // 将使用说明作为补全结果返回
        completion.complete(Collections.singletonList(usage));
    }

    /**
     * 私有补全方法
     *
     * 该方法根据候选列表的数量和最长公共前缀进行智能补全：
     * - 如果只有一个候选，直接补全
     * - 如果有多个候选但有公共前缀，补全公共前缀
     * - 否则显示所有候选
     *
     * @param completion 补全对象
     * @param prefix 用户输入的前缀
     * @param candidates 候选列表
     */
    private static void complete(Completion completion, String prefix, List<String> candidates) {
        if (candidates.size() == 1) {
            // 只有一个候选，直接补全
            completion.complete(candidates.get(0).substring(prefix.length()), true);
        } else {
            // 多个候选，查找最长公共前缀
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(candidates);
            if (commonPrefix.length() > 0) {
                // 如果有公共前缀
                if (commonPrefix.length() == prefix.length()) {
                    // 如果公共前缀等于用户输入的前缀，说明无法继续补全，显示所有候选
                    completion.complete(candidates);
                } else {
                    // 否则补全公共前缀
                    completion.complete(commonPrefix.substring(prefix.length()), false);

                }
            } else {
                // 没有公共前缀，显示所有候选
                completion.complete(candidates);
            }
        }
    }

    /**
     * <pre>
     * 检查是否应该补全某个 option。
     * 比如 option是： --classPattern ， tokens可能是：
     *  2个： '--classPattern' ' '
     *  3个： '--classPattern' ' ' 'demo.'
     * </pre>
     *
     * 该方法检查命令行tokens是否匹配给定的选项，如果匹配，则进行类名补全
     *
     * @param completion 补全对象
     * @param option 选项名称
     * @return 是否进行了补全
     */
    public static boolean shouldCompleteOption(Completion completion, String option) {
        List<CliToken> tokens = completion.lineTokens();
        // 有两个 token, 然后 倒数第一个不是 - 开头的
        // 情况1: tokens = ['--classPattern', ' ']
        if (tokens.size() >= 2) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            String token_2 = cliToken_2.value();
            // 如果倒数第一个token不是以"-"开头，且倒数第二个token等于给定选项
            if (!cliToken_1.value().startsWith("-") && token_2.equals(option)) {
                return CompletionUtils.completeClassName(completion);
            }
        }
        // 有三个 token，然后 倒数第一个不是 - 开头的，倒数第2是空的，倒数第3是 --classPattern
        // 情况2: tokens = ['--classPattern', ' ', 'demo.']
        if (tokens.size() >= 3) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            CliToken cliToken_3 = tokens.get(tokens.size() - 3);
            // 如果倒数第一个token不是以"-"开头，倒数第二个token为空，倒数第三个token等于给定选项
            if (!cliToken_1.value().startsWith("-") && cliToken_2.isBlank()
                    && cliToken_3.value().equals(option)) {
                return CompletionUtils.completeClassName(completion);
            }
        }
        return false;
    }

    /**
     * 根据处理器列表完成选项的自动补全
     *
     * 该方法检查命令行tokens，找到需要补全的选项，然后调用对应的处理器进行补全
     *
     * @param completion 补全对象
     * @param handlers 选项补全处理器列表
     * @return 是否成功完成补全
     */
    public static boolean completeOptions(Completion completion, List<OptionCompleteHandler> handlers) {
        List<CliToken> tokens = completion.lineTokens();
        /**
         * <pre>
         * 比如 ` --name a`，这样子的tokens
         * tokens = ['--name', ' ', 'a']
         * </pre>
         */
        if (tokens.size() >= 3) {
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            CliToken cliToken_3 = tokens.get(tokens.size() - 3);

            // 如果倒数第二个token为空，说明倒数第三个token是选项名
            if (cliToken_2.isBlank()) {
                String token_3 = cliToken_3.value();

                // 查找匹配的处理器并执行补全
                for (OptionCompleteHandler handler : handlers) {
                    if (handler.matchName(token_3)) {
                        return handler.complete(completion);
                    }
                }
            }
        }

        /**
         * <pre>
         * 比如 ` --name `，这样子的tokens
         * tokens = ['--name', ' ']
         * </pre>
         */
        if (tokens.size() >= 2) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            // 如果倒数第一个token为空，说明倒数第二个token是选项名
            if (cliToken_1.isBlank()) {
                String token_2 = cliToken_2.value();
                // 查找匹配的处理器并执行补全
                for (OptionCompleteHandler handler : handlers) {
                    if (handler.matchName(token_2)) {
                        return handler.complete(completion);
                    }
                }
            }
        }

        return false;
    }
}
