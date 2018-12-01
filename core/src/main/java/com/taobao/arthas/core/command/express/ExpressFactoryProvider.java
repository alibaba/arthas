package com.taobao.arthas.core.command.express;

/**
 *  SPI for ExpressFactory
 * @author qxo 2018-12-01
 *
 */
public interface ExpressFactoryProvider {

	public Express createExpress(ClassLoader classloader) ;
	
	public String getPreifx();
}
