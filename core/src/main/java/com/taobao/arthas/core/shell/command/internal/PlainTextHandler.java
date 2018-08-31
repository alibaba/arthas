package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.text.util.RenderUtil;

import java.util.List;

/**
 * @author beiwei30 on 20/12/2016.
 */
public class PlainTextHandler extends StdoutHandler {
    public static String NAME = "plaintext";

    public static StdoutHandler inject(List<CliToken> tokens) {
        return new PlainTextHandler();
    }

    @Override
    public String apply(String s) {
        return RenderUtil.ansiToPlainText(s);
    }
}
