package com.taobao.arthas.core.command.express;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

/**
 *  ExpressFactoryProvider4jsr223 
 *  
 * @author qxo 2018-12-01
 *
 */
public class ExpressFactoryProvider4jsr223 implements ExpressFactoryProvider {
	   private static final Logger logger = LogUtil.getArthasLogger();

	private final  String prefix ;
   
	static final ScriptEngineManager manager = new ScriptEngineManager(ExpressFactoryProvider.class.getClassLoader());
	 
	protected final ScriptEngine scriptEngine;
	 
	public ExpressFactoryProvider4jsr223( final String prefix) {
		this(prefix,null);
	}
	public ExpressFactoryProvider4jsr223( final String prefix,final String defaultScript) {
		super();
		this.prefix = prefix;		
        scriptEngine = manager.getEngineByName(prefix);
        if( scriptEngine == null ) {
        	throw new IllegalStateException("JSR-223 script engine not found:"+prefix);
        }
        if( defaultScript != null && defaultScript.length() >0 ) {
        	ScriptContext globalContext = scriptEngine.getContext();
            try {
            	scriptEngine.eval(defaultScript,globalContext );
			} catch (ScriptException e) {
				logger.warn(e.getMessage(), e);
			}
        }
	}

	@Override
	public Express createExpress(ClassLoader classloader) {
		return classloader == null ?  new JSR223Express(scriptEngine,null) :  new JSR223Express(scriptEngine,classloader);
	}
	
	public  final String getPreifx() {
		return prefix;
	}
}