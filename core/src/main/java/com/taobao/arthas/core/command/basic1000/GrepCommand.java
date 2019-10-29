package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @see com.taobao.arthas.core.shell.command.internal.GrepHandler
 */
@Name("grep")
@Summary("grep command for pipes (-e -m -n -v -A -B -C -f )\n" )
@Description(Constants.EXAMPLE +
        "sysprop | grep java \n" +
        " sysenv | grep -v JAVA -n\n" +
        " sysenv | grep -e \"(?i)(JAVA|sun)\" -m 3  -C 2\n" +
        " sysenv | grep -v JAVA -A2 -B3\n" +
        " sysenv | grep  -e JAVA -f /tmp/t.log \n" +
        " thread | grep -m 10 -e  \"TIMED_WAITING|WAITING\"\n\n"
  +"-e, --regexp              use PATTERN for matching\n"
  +"-m, --max-count=NUM       stop after NUM selected lines\n"
  +"-n, --line-number         print line number with output lines\n"
  +"-v, --invert-match        select non-matching lines\n"
  +"-A, --after-context=NUM   print NUM lines of trailing context\n"
  +"-B, --before-context=NUM  print NUM lines of leading context\n"
  +"-C, --context=NUM         print NUM lines of output context\n"
//  +"-f, --output=File         output result to file, filename endsWith :false for disable append mode\n"
  + Constants.WIKI + Constants.WIKI_HOME + "grep")
public class GrepCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        process.write("The grep command only for pipes ").write("\n");
        final Description ann = GrepCommand.class.getAnnotation(Description.class);
        if (ann != null) {
          process.write(ann.value()).write("\n");
        }
        process.end();
    }
}
