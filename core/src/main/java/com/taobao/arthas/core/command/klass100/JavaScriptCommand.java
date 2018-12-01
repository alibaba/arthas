package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.Constants;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 *
 * @author qxo  2018-12-01
 *
 */
@Name("js")
@Summary("Execute js expression.")
@Description(Constants.EXAMPLE
                + "  js 'java.lang.System.out.println(\"hello\")' \n"
                + "  js -x 2 'Singleton.getInstance()' \n"
                + "  js 'it.appClass(\"Demo\").staticFiled' \n"
                + "  js 'var value1=System.getProperty(\"java.home\");var value2=System.getProperty(\"java.runtime.name\");[value1, value2]'\n"
                + "  js -c 5d113a51 'it.appClass(\"com.taobao.arthas.core.GlobalOptions\").isDump' \n"
                + Constants.WIKI + Constants.WIKI_HOME + "\n"
                + " https://developer.oracle.com/databases/nashorn-javascript-part1\n https://www.n-k.de/riding-the-nashorn/")
public class JavaScriptCommand extends ElCommand  {

	public JavaScriptCommand() {
		super("js");
	}
}
