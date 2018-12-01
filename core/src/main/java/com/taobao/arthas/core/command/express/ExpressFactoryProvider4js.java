package com.taobao.arthas.core.command.express;

/**
 *  ExpressFactoryProvider4jsr223 for js 
 * @author qxo 2018-12-01
 *
 */
public class ExpressFactoryProvider4js extends ExpressFactoryProvider4jsr223 {

	public ExpressFactoryProvider4js() {
		super("js","load(\"nashorn:mozilla_compat.js\");importPackage(java.lang);");
	}
}