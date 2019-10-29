package com.taobao.arthas.core.shell.command.internal;

import java.util.List;
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
    private static final Pattern TRIM_PATTERN;
    static {
       //默认删除右边的空白字符是为了解决-n 因空白字符导致显示换行的问题
       //ie: sysprop | grep -n java
       final String p = System.getProperty("arthas_grep_trim_pattern", "[ \\f\t\\v]+$");
       TRIM_PATTERN = "NOP".equals(p) ? null : Pattern.compile(p);
    }

    private String keyword;
    private boolean ignoreCase;
    // -v, --invert-match        select non-matching lines
    private final boolean invertMatch;
    //-e, --regexp=PATTERN      use PATTERN for matching
    private final Pattern pattern;
    // -n, --line-number         print line number with output lines
    private final boolean showLineNumber;
/*
  -B, --before-context=NUM  print NUM lines of leading context
  -A, --after-context=NUM   print NUM lines of trailing context
  -C, --context=NUM         print NUM lines of output context
 */
    private final int beforeLines;
    private final int afterLines;
    //-m, --max-count=NUM       stop after NUM selected lines
    private final int maxCount;
    
    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        CommandLine commandLine = CLIs.create(NAME)
                .addOption(new Option().setShortName("i").setLongName("ignore-case").setFlag(true))
                .addOption(new Option().setShortName("v").setLongName("invert-match").setFlag(true))
                .addOption(new Option().setShortName("n").setLongName("line-number").setFlag(true))
                .addOption(new Option().setShortName("B").setLongName("before-context").setSingleValued(true))
                .addOption(new Option().setShortName("A").setLongName("after-context").setSingleValued(true))
                .addOption(new Option().setShortName("C").setLongName("context").setSingleValued(true))
                .addOption(new Option().setShortName("e").setLongName("regexp").setFlag(true))
                .addOption(new Option().setShortName("f").setLongName("output").setSingleValued(true))
                .addOption(new Option().setShortName("m").setLongName("max-count").setSingleValued(true))
                .addArgument(new Argument().setArgName("keyword").setIndex(0))
                .parse(args);
        Boolean ignoreCase = commandLine.isFlagEnabled("ignore-case");
        String keyword = commandLine.getArgumentValue(0);
        final boolean invertMatch = commandLine.isFlagEnabled("invert-match");
        final boolean regexpMode = commandLine.isFlagEnabled("regexp");
        final boolean showLineNumber =  commandLine.isFlagEnabled("line-number");
        int context =  getInt(commandLine, "context", 0);
        int beforeLines = getInt(commandLine, "before-context", 0);
        int afterLines = getInt(commandLine, "after-context", 0);
        if (context > 0) {
          if (beforeLines < 1) {
            beforeLines = context;
          }
          if (afterLines < 1 ){
            afterLines = context;
          }
        }
        final int maxCount = getInt(commandLine, "max-count", 0);
        return new GrepHandler(keyword, ignoreCase, invertMatch, regexpMode, showLineNumber
            , beforeLines, afterLines, maxCount);
    }
    
    private static final int getInt(CommandLine cmdline,  String name , int defaultValue) {
        final String v = cmdline.getOptionValue(name);
        final int ret = v== null ? defaultValue : Integer.parseInt(v);
        return ret;
    }

    private GrepHandler(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode
        , boolean showLineNumber,  int beforeLines, int afterLines,int maxCount) {
        this.ignoreCase = ignoreCase;
        this.invertMatch = invertMatch;
        this.showLineNumber = showLineNumber;
        this.beforeLines = beforeLines > 0 ? beforeLines : 0;
        this.afterLines = afterLines > 0 ? afterLines : 0;
        this.maxCount = maxCount > 0 ? maxCount : 0;
        if (regexpMode) {
           final int flags = ignoreCase ?  Pattern.CASE_INSENSITIVE : 0;
           this.pattern = Pattern.compile(keyword, flags);
        } else {
           this.pattern = null;
        }
        this.keyword = ignoreCase ?  keyword.toLowerCase() : keyword;
    }

    @Override
    public String apply(String input) {
        StringBuilder output = new StringBuilder();
        String[] lines = input.split("\n");
        int continueCount = 0 ;
        int lastStartPos = 0 ;
        int lastContinueLineNum = -1;
        int matchCount = 0;
        for (int lineNum = 0 ; lineNum < lines.length ;) {
          String line  = TRIM_PATTERN.matcher(lines[lineNum++]).replaceAll("");
          final boolean match;
          if (pattern == null) {
            match = (ignoreCase ?  line.toLowerCase() : line).contains(keyword);
          } else {
            match = pattern.matcher(line).find();
          }
          if (invertMatch ? !match : match) {
            matchCount++;
            if (beforeLines > continueCount) {
              int n = lastContinueLineNum == -1 ? ( beforeLines >=  lineNum  ?  1  : lineNum - beforeLines )
                 : lineNum - beforeLines  - continueCount;
              if ( n >= lastContinueLineNum ||  lastContinueLineNum == -1 ) {
                StringBuilder  beforeSb = new StringBuilder();
                for (int i = n ; i < lineNum  ; i++) {
                    appendLine(beforeSb, i, lines[i - 1]);
                }
                output.insert(lastStartPos, beforeSb);
              }
            } // end handle before lines

            lastStartPos = output.length();
            appendLine(output, lineNum, line);

            if (afterLines > continueCount) {
              int  last  = lineNum +  afterLines - continueCount;
              if (last > lines.length) {
                   last = lines.length;
              }
               for(int i = lineNum ; i < last ; i++) {
                    appendLine(output, i+1, lines[i]);
                    lineNum ++; 
                    continueCount++;
                    lastStartPos = output.length();
              }
            } //end handle afterLines
            
            continueCount++;
            if(maxCount > 0 && matchCount >= maxCount) {
                break;
            }
          } else { // not match
            if(continueCount > 0) {
              lastContinueLineNum = lineNum -1 ;
              continueCount = 0;
            }
          }
        }
        final String str = output.toString();// output.length() > 0 ? output.substring(0, output.length()-1) : "";
        return str;
    }

    protected void appendLine(StringBuilder output, int lineNum, String line) {
      if(showLineNumber) {
        output.append(lineNum).append(':');
        }
      output.append(line).append('\n');
    }
}
