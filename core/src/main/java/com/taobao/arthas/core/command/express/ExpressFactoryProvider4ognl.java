package com.taobao.arthas.core.command.express;

/**
 *  ExpressFactoryProvider for ognl 
 *  
 * @author qxo 2018-12-01
 *
 */
public class ExpressFactoryProvider4ognl implements ExpressFactoryProvider {

	@Override
	public Express createExpress(ClassLoader classloader) {
		return classloader == null ?  new OgnlExpress() :  new OgnlExpress(new ClassLoaderClassResolver(classloader));
	}
	
	public String getPreifx() {
		return "ognl";
	}
}