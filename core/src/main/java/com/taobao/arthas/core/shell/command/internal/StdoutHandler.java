package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.cli.CliToken;
import io.termd.core.function.Function;

import java.util.LinkedList;
import java.util.List;

/**
 * @author beiwei30 on 20/12/2016.
 */
public abstract class StdoutHandler implements Function<String, String> {

    public static StdoutHandler inject(List<CliToken> tokens) {
        CliToken firstTextToken = null;
        for (CliToken token : tokens) {
            if (token.isText()) {
                firstTextToken = token;
                break;
            }
        }

        if (firstTextToken == null) {
            return null;
        }

        if (firstTextToken.value().equals(GrepHandler.NAME)) {
            return GrepHandler.inject(tokens);
        } else if (firstTextToken.value().equals(PlainTextHandler.NAME)) {
            return PlainTextHandler.inject(tokens);
        } else if (firstTextToken.value().equals(WordCountHandler.NAME)) {
            return WordCountHandler.inject(tokens);
        } else if (firstTextToken.value().equals(TeeHandler.NAME)){
            return TeeHandler.inject(tokens);
        } else{
            return null;
        }
    }

    public static List<String> parseArgs(List<CliToken> tokens, String command) {
        List<String> args = new LinkedList<String>();
        boolean found = false;
        for (CliToken token : tokens) {
            if (token.isText() && token.value().equals(command)) {
                found = true;
            } else if (token.isText() && found) {
                args.add(token.value());
            }
        }
        return args;
    }

    @Override
    public String apply(String s) {
        return s;
    }
}
