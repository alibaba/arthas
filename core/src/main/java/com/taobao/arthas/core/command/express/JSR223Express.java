package com.taobao.arthas.core.command.express;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 *  ExpressFactoryProvider for ognl 
 *  
 * @author qxo 2018-12-01
 *
 */
public class JSR223Express extends AbstractExpress {

	public static final  class CustomBinding extends AbstractMap<String, Object> implements Bindings {

		private final ScriptContext globalBindings;
		
		 private Map<String,Object> map;
		public Object put(String key, Object value) {
			return map.put(key, value);
		}

		public CustomBinding(ScriptContext globalBindings) {
			 map = new HashMap<String,Object> ();
			this.globalBindings = globalBindings;
		}
		
		@Override
		public Set<Entry<String, Object>> entrySet() {
			return map.entrySet();
		}
		
		@Override
		public boolean containsKey(Object key) {
			if( key == null ) {
				return false;
			}
			boolean ret =  map.containsKey(key);
			if( !ret && globalBindings != null  ) {
				ret = findByKey(key) != null;
			}
			return ret;
		}

		protected Object findByKey(Object  key) {
			final Object v =  globalBindings.getAttribute((String)key, ScriptContext.ENGINE_SCOPE);
			return v;
		}
		
		public Class<?> getClass(String cls) throws ClassNotFoundException {
			return  Thread.currentThread().getContextClassLoader().loadClass(cls);
		}

		public Class<?> appClass(String cls) throws ClassNotFoundException {
			return  getClass(cls);
		}
		
		public void println(Object v) {
			System.out.println(v);
		}
		
		@Override
		public Object get(Object key) {
			if( key == null ) {
				return null;
			}
			Object ret = map.get(key);
			if( ret == null && globalBindings != null  ) {
				ret = findByKey((String)key) ;
			}
			return ret;
		}
		
	}

    private final Bindings context;

    protected final ScriptEngine scriptEngine;

    private final ClassLoader classLoader;
    public JSR223Express(ScriptEngine scriptEngine,ClassLoader classLoader) {
        context = new CustomBinding(scriptEngine.getContext());
        context.put("it", context);
        this.scriptEngine = scriptEngine;
        this.classLoader = classLoader; 
    }
    
    @Override
    public Object get(String express) throws ExpressException {
    	Thread currentThread = Thread.currentThread();
    	final ClassLoader old = classLoader == null ? null :currentThread.getContextClassLoader();
    	if( classLoader != null ) {
    		currentThread.setContextClassLoader(classLoader);
    	}
        try {
        	  return  scriptEngine.eval(express, context);
        } catch (Exception e) {
            logger.error(null, "Error during evaluating the expression:", e);
            throw new ExpressException(express, e);
        }finally {
        	if( classLoader != null ) {
        		currentThread.setContextClassLoader(old);
        	}
        }
    }

    @Override
    public Express bind(String name, Object value) {
        context.put(name, value);
        return this;
    }

    @Override
    public Express reset() {
        context.clear();
        return this;
    }
}
