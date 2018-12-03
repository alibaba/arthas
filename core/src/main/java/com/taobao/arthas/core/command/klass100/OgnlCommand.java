package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.Constants;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 *
 * @author hengyunabc 2018-10-18
 *
 */
@Name("ognl")
@Summary("Execute ognl expression.")
@Description(Constants.EXAMPLE
                + "  ognl '@java.lang.System@out.println(\"hello\")' \n"
                + "  ognl -x 2 '@Singleton@getInstance()' \n"
                + "  ognl '@Demo@staticFiled' \n"
                + "  ognl '#value1=@System@getProperty(\"java.home\"), #value2=@System@getProperty(\"java.runtime.name\"), {#value1, #value2}'\n"
                + "  ognl -c 5d113a51 '@com.taobao.arthas.core.GlobalOptions@isDump' \n"
                + Constants.WIKI + Constants.WIKI_HOME + "ognl\n"
                + "  https://commons.apache.org/proper/commons-ognl/language-guide.html")
public class OgnlCommand extends ElCommand  {

	public OgnlCommand() {
		super("ognl");
	}
	
}
