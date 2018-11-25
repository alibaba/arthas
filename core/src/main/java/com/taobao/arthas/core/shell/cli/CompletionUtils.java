package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import io.termd.core.util.Helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
        CliToken lastToken = tokens.get(tokens.size() - 1);
        List<String> candidates = new ArrayList<String>();
        for (String name: searchScope) {
            if (" ".equals(lastToken.value()) || name.startsWith(lastToken.value())) {
                candidates.add(name);
            }
        }
        if (candidates.size() == 1) {
            completion.complete(candidates.get(0).substring(lastToken.value().length()), true);
            return true;
        } else {
            completion.complete(candidates);
            return false;
        }
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
                    completion.complete(commonPrefix.substring(prefix.length(), commonPrefix.length()), false);
                }

            } else {
                completion.complete(candidates);
            }
        }
    }
}
