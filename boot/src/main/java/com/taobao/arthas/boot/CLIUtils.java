package com.taobao.arthas.boot;

import com.taobao.arthas.common.UsageRender;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.UsageMessageFormatter;

/**
 *
 * @author hengyunabc 2019-03-26
 *
 */
public class CLIUtils {
    public static String usage(CLI cli) {
        StringBuilder usageStringBuilder = new StringBuilder();
        UsageMessageFormatter usageMessageFormatter = new UsageMessageFormatter();
        usageMessageFormatter.setOptionComparator(null);
        cli.usage(usageStringBuilder, usageMessageFormatter);
        return UsageRender.render(usageStringBuilder.toString());
    }
}
