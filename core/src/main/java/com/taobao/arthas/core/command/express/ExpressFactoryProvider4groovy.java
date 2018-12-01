package com.taobao.arthas.core.command.express;


/**
 *  ExpressFactoryProvider4jsr223 for groovy 
 * @author qxo 2018-12-01
 *
 */
public class ExpressFactoryProvider4groovy extends ExpressFactoryProvider4jsr223 {

	public ExpressFactoryProvider4groovy() {
		super("groovy",null);
		this.scriptEngine.put("#jsr223.groovy.engine.keep.globals", "weak");
	}


}