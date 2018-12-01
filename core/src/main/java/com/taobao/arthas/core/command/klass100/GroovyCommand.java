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
@Name("groovy")
@Summary("Execute groovy expression.")
@Description(Constants.EXAMPLE
                + "  groovy 'java.lang.System.out.println(\"hello\")' \n"
                + "  groovy -x 2 'Singleton.getInstance()' \n"
                + "  groovy 'it.appClass(\"Demo\").staticFiled' \n"
                + "  groovy 'def value1=System.getProperty(\"java.home\");def value2=System.getProperty(\"java.runtime.name\");[value1, value2]'\n"
                + "  groovy -c 5d113a51 'it.appClass(\"com.taobao.arthas.core.GlobalOptions\").isDump' \n"
                + Constants.WIKI + Constants.WIKI_HOME + "\n"
                + " http://groovy-lang.org/single-page-documentation.html")
public class GroovyCommand extends ElCommand  {

	public GroovyCommand() {
		super("groovy");
	}
}