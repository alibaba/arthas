package com.taobao.arthas.core.util;

import com.taobao.arthas.core.shell.command.CommandProcess;

import java.util.Timer;

/**
 * Date: 2019/4/24
 *
 * @author xuzhiyi
 */
public class TimerUtils {

    public static Timer create(CommandProcess process) {
        return new Timer("Timer-for-arthas-mbean-" + process.session().getSessionId(), true);

    }
}
