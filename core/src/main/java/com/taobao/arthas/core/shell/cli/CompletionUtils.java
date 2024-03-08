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
 * @author beiwei30 on 09/11/2016.
 */
public class CompletionUtils {

    public static String findLongestCommonPrefix(Collection<String> values) {
        List<int[]> entries = new LinkedList<int[]>();
        for (String value : values) {
            int[] entry = Helper.toCodePoints(value);
            entries.add(entry);
        }
        return Helper.fromCodePoints(io.termd.core.readline.Completion.findLongestCommonPrefix(entries));
    }

    public static void complete(Completion completion, Class<?> clazz) {
        List<CliToken> tokens = completion.lineTokens();
        CliToken lastToken = tokens.get(tokens.size() - 1);
        CLI cli = CLIConfigurator.define(clazz);
        List<com.taobao.middleware.cli.Option> options = cli.getOptions();
        if (lastToken == null || lastToken.isBlank()) {
            completeUsage(completion, cli);
        } else if (lastToken.value().startsWith("--")) {
            completeLongOption(completion, lastToken, options);
        } else if (lastToken.value().startsWith("-")) {
            completeShortOption(completion, lastToken, options);
        } else {
            completion.complete(Collections.<String>emptyList());
        }
    }

    /**
     * 从给定的查询数组中查询匹配的对象，并进行自动补全
     */
    public static boolean complete(Completion completion, Collection<String> searchScope) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = tokens.get(tokens.size() - 1).value();
        List<String> candidates = new ArrayList<String>();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        for (String name: searchScope) {
            if (name.startsWith(lastToken)) {
                candidates.add(name);
            }
        }
        if (candidates.size() == 1) {
            completion.complete(candidates.get(0).substring(lastToken.length()), true);
            return true;
        } else {
            completion.complete(candidates);
            return true;
        }
    }

    private static boolean isEndOfDirectory(String token) {
        return !StringUtils.isBlank(token) && (token.endsWith(File.separator) || token.endsWith("/"));
    }

    /**
     * 返回true表示已经完成completion，返回否则表示没有，调用者需要另外完成补全
     * @param completion
     * @return
     */
    public static boolean completeFilePath(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String token = tokens.get(tokens.size() - 1).value();

        if (token.startsWith("-") || StringUtils.isBlank(token)) {
            return false;
        }

        File dir = null;
        String partName = "";
        if (StringUtils.isBlank(token)) {
            dir = new File("").getAbsoluteFile();
            token = "";
        } else if (isEndOfDirectory(token)) {
            dir = new File(token);
        } else {
            File parent = new File(token).getAbsoluteFile().getParentFile();
            if (parent != null && parent.exists()) {
                dir = parent;
                partName = new File(token).getName();
            }
        }

        File tokenFile = new File(token);

        String tokenFileName = null;
        if (isEndOfDirectory(token)) {
            tokenFileName = "";
        } else {
            tokenFileName = tokenFile.getName();
        }

        if (dir == null) {
            return false;
        }

        File[] listFiles = dir.listFiles();

        ArrayList<String> names = new ArrayList<>();
        if (listFiles != null) {
            for (File child : listFiles) {
                if (child.getName().startsWith(partName)) {
                    if (child.isDirectory()) {
                        names.add(child.getName() + "/");
                    } else {
                        names.add(child.getName());
                    }
                }
            }
        }

        if (names.size() == 1 && isEndOfDirectory(names.get(0))) {
            String name = names.get(0);
            // 这个函数补全后不会有空格，并且只能传入要补全的内容
            completion.complete(name.substring(tokenFileName.length()), false);
            return true;
        }

        String prefix = null;
        if (isEndOfDirectory(token)) {
            prefix = token;
        } else {
            prefix = token.substring(0, token.length() - new File(token).getName().length());
        }

        ArrayList<String> namesWithPrefix = new ArrayList<>();
        for (String name : names) {
            namesWithPrefix.add(prefix + name);
        }
        // 这个函数需要保留前缀
        CompletionUtils.complete(completion, namesWithPrefix);
        return true;
    }

    public static boolean completeClassName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = tokens.get(tokens.size() - 1).value();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        if (lastToken.startsWith("-")) {
            return false;
        }

        Instrumentation instrumentation = completion.session().getInstrumentation();

        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();

        Set<String> result = new HashSet<String>();
        for(Class<?> clazz : allLoadedClasses) {
            String name = clazz.getName();
            if (name.startsWith("[")) {
                continue;
            }
            if(name.startsWith(lastToken)) {
                int index = name.indexOf('.', lastToken.length());

                if(index > 0) {
                    result.add(name.substring(0, index + 1));
                }else {
                    result.add(name);
                }

            }
        }

        if(result.size() == 1 && result.iterator().next().endsWith(".")) {
            completion.complete(result.iterator().next().substring(lastToken.length()), false);
        }else {
            CompletionUtils.complete(completion, result);
        }
        return true;
    }

    public static boolean completeMethodName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = completion.lineTokens().get(tokens.size() - 1).value();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        // retrieve the class name
        String className;
        if (StringUtils.isBlank(lastToken)) {
            className = tokens.get(tokens.size() - 2).value();
        } else {
            className = tokens.get(tokens.size() - 3).value();
        }

        Set<Class<?>> results = SearchUtils.searchClassOnly(completion.session().getInstrumentation(), className, 2);
        if (results.size() != 1) {
            // no class found or multiple class found
            completion.complete(Collections.<String>emptyList());
            return true;
        }

        Class<?> clazz = results.iterator().next();

        List<String> res = new ArrayList<String>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (StringUtils.isBlank(lastToken)) {
                res.add(method.getName());
            } else if (method.getName().startsWith(lastToken)) {
                res.add(method.getName());
            }
        }

        if (res.size() == 1) {
            completion.complete(res.get(0).substring(lastToken.length()), true);
            return true;
        } else {
            CompletionUtils.complete(completion, res);
            return true;
        }
    }

    /**
     * 推断输入到哪一个 argument
     * @param completion
     * @return
     */
    public static int detectArgumentIndex(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        CliToken lastToken = tokens.get(tokens.size() - 1);

        if (lastToken.value().startsWith("-") || lastToken.value().startsWith("--")) {
            return -1;
        }

        if (StringUtils.isBlank((lastToken.value())) && tokens.size() == 1) {
            return 1;
        }

        int tokenCount = 0;

        for (CliToken token : tokens) {
            if (StringUtils.isBlank(token.value()) || token.value().startsWith("-") || token.value().startsWith("--")) {
                // filter irrelevant tokens
                continue;
            }
            tokenCount++;
        }

        if (StringUtils.isBlank((lastToken.value())) && tokens.size() != 1) {
            tokenCount++;
        }
        return tokenCount;
    }

    public static void completeShortOption(Completion completion, CliToken lastToken, List<Option> options) {
        String prefix = lastToken.value().substring(1);
        List<String> candidates = new ArrayList<String>();
        for (Option option : options) {
            if (option.getShortName().startsWith(prefix)) {
                candidates.add(option.getShortName());
            }
        }
        complete(completion, prefix, candidates);
    }

    public static void completeLongOption(Completion completion, CliToken lastToken, List<Option> options) {
        String prefix = lastToken.value().substring(2);
        List<String> candidates = new ArrayList<String>();
        for (Option option : options) {
            if (option.getLongName().startsWith(prefix)) {
                candidates.add(option.getLongName());
            }
        }
        complete(completion, prefix, candidates);
    }

    public static void completeUsage(Completion completion, CLI cli) {
        Tty tty = completion.session().get(Session.TTY);
        String usage = StyledUsageFormatter.styledUsage(cli, tty.width());
        completion.complete(Collections.singletonList(usage));
    }

    private static void complete(Completion completion, String prefix, List<String> candidates) {
        if (candidates.size() == 1) {
            completion.complete(candidates.get(0).substring(prefix.length()), true);
        } else {
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(candidates);
            if (commonPrefix.length() > 0) {
                if (commonPrefix.length() == prefix.length()) {
                    completion.complete(candidates);
                } else {
                    completion.complete(commonPrefix.substring(prefix.length()), false);
                }

            } else {
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
     * @param option
     * @return
     */
    public static boolean shouldCompleteOption(Completion completion, String option) {
        List<CliToken> tokens = completion.lineTokens();
        // 有两个 tocken, 然后 倒数第一个不是 - 开头的
        if (tokens.size() >= 2) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            String token_2 = cliToken_2.value();
            if (!cliToken_1.value().startsWith("-") && token_2.equals(option)) {
                return CompletionUtils.completeClassName(completion);
            }
        }
        // 有三个 token，然后 倒数第一个不是 - 开头的，倒数第2是空的，倒数第3是 --classPattern
        if (tokens.size() >= 3) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            CliToken cliToken_3 = tokens.get(tokens.size() - 3);
            if (!cliToken_1.value().startsWith("-") && cliToken_2.isBlank()
                    && cliToken_3.value().equals(option)) {
                return CompletionUtils.completeClassName(completion);
            }
        }
        return false;
    }

    public static boolean completeOptions(Completion completion, List<OptionCompleteHandler> handlers) {
        List<CliToken> tokens = completion.lineTokens();
        /**
         * <pre>
         * 比如 ` --name a`，这样子的tokens
         * </pre>
         */
        if (tokens.size() >= 3) {
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            CliToken cliToken_3 = tokens.get(tokens.size() - 3);

            if (cliToken_2.isBlank()) {
                String token_3 = cliToken_3.value();

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
         * </pre>
         */
        if (tokens.size() >= 2) {
            CliToken cliToken_1 = tokens.get(tokens.size() - 1);
            CliToken cliToken_2 = tokens.get(tokens.size() - 2);
            if (cliToken_1.isBlank()) {
                String token_2 = cliToken_2.value();
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
