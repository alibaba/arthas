package com.taobao.arthas.core.shell.command.internal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.middleware.cli.Argument;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;

/**
 * @author beiwei30 on 12/12/2016.
 */
public class GrepHandler extends StdoutHandler {
    public static final String NAME = "grep";
    //强制指定输出文件后缀名是为了防止无意识输错文件名而覆盖现有价值的现有文件
    private static final String FORCE_OUTPUT_SUFFIX = ".log";
    //output file name with :false for disable append mode
    private static final Pattern APPEND_FLAG_PATTERN = Pattern.compile("[:#;](true|false)$");

    private String keyword;
    private boolean ignoreCase;
    // -v, --invert-match        select non-matching lines
    private final boolean invertMatch;
    //-e, --regexp=PATTERN      use PATTERN for matching
    private final Pattern pattern;
    private final String outputFile;
    private final boolean outputAppend;
    
    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        CommandLine commandLine = CLIs.create(NAME)
                .addOption(new Option().setShortName("i").setLongName("ignore-case").setFlag(true))
                .addOption(new Option().setShortName("v").setLongName("invert-match").setFlag(true))
                .addOption(new Option().setShortName("e").setLongName("regexp").setFlag(true))
                .addOption(new Option().setShortName("f").setLongName("output").setSingleValued(true))
                .addArgument(new Argument().setArgName("keyword").setIndex(0))
                .parse(args);
        Boolean ignoreCase = commandLine.isFlagEnabled("ignore-case");
        String keyword = commandLine.getArgumentValue(0);
        final boolean invertMatch = commandLine.isFlagEnabled("invert-match");
        final boolean regexpMode = commandLine.isFlagEnabled("regexp");
        final String output = commandLine.getOptionValue("output");
        return new GrepHandler(keyword, ignoreCase, invertMatch, regexpMode, output);
    }

    private GrepHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode, String output) {
        this.ignoreCase = ignoreCase;
        this.invertMatch = invertMatch;
        if (regexpMode) {
           final int flags = ignoreCase ?  Pattern.CASE_INSENSITIVE : 0;
           this.pattern = Pattern.compile(keyword, flags);
        } else {
           this.pattern = null;
        }
        this.keyword = ignoreCase ?  keyword.toLowerCase() : keyword;
        if (output != null && output.length() > 0) {
          final Matcher matcher = APPEND_FLAG_PATTERN.matcher(output);
          if (matcher.find()) {
            outputAppend = Boolean.parseBoolean(matcher.group(1)) ;
            output = matcher.replaceAll("");
          }else {
            outputAppend = true;
          }
          if(!output.endsWith(FORCE_OUTPUT_SUFFIX)) {
            output += FORCE_OUTPUT_SUFFIX;
          }
        } else {
          output = null;
          outputAppend = true;
        }
        this.outputFile = output;
    }

    @Override
    public String apply(String input) {
        StringBuilder output = new StringBuilder();
        String[] lines = input.split("\n");
        PrintWriter writer  = null;
        try {
          if (outputFile != null) {
            writer = new PrintWriter(new FileOutputStream(outputFile, outputAppend), true);
          }
          for (String line : lines) {
            final boolean match;
            if (pattern == null) {
              match = (ignoreCase ?  line.toLowerCase() : line).contains(keyword);
            } else {
              match = pattern.matcher(line).find();
            }
            if (invertMatch ? !match : match) {
              output.append(line).append("\n");
              if (writer != null) {
                writer.println(line);
              }
            }
          }
        }catch(IOException ex) {
          throw new IllegalStateException(ex);
        }finally {
          if (writer != null) {
            writer.close();
          }
        }
        return output.toString();
    }
}
