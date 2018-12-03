package com.taobao.arthas.core.command.express;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

/**
 * ExpressFactory
 * @author ralf0131 2017-01-04 14:40.
 * @author hengyunabc 2018-10-08
 */
public class ExpressFactory {
    private static final Logger logger = LogUtil.getArthasLogger();

    private static InstHolder inst =  new InstHolder();
    
    protected static InstHolder getInst() {
    	if( inst == null ) {
    		inst =  new InstHolder();
    	}
    	return inst;
    }
    public static void reset() {
    	if( inst != null ) {
    		inst = null;
    	}
    }
    public static String getDefaultEL() {
    	return getInst().DEFAULT_EL;
    }
    public static final boolean JSR_223_DEFAULT_ON = Boolean.getBoolean("arthas_jsr223_on");
    
    private static final class InstHolder {

    private  final ExpressFactoryProvider defaultProvider ;
	private  final Map<String,ExpressFactoryProvider> providerMapping = new HashMap<String,ExpressFactoryProvider>();
	private  final Pattern elPrefixPattern ;
	 public  final String DEFAULT_EL;
	 {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader oldClassLoader = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(ExpressFactory.class.getClassLoader());
			Iterator<ExpressFactoryProvider> iterator = ServiceLoader.load(ExpressFactoryProvider.class,ExpressFactory.class.getClassLoader()).iterator();
			while(iterator.hasNext()){
				try {
					ExpressFactoryProvider p = iterator.next();
					providerMapping.put(p.getPreifx(), p);
				}catch(Throwable ex) {
					logger.warn(ex.getMessage());
				}
			}
			if(JSR_223_DEFAULT_ON ) {
				
			List<ScriptEngineFactory> engineFactories = ExpressFactoryProvider4jsr223.manager.getEngineFactories();
			label1:
			for (ScriptEngineFactory scriptEngineFactory : engineFactories) {
				String engineName = scriptEngineFactory.getLanguageName();
				if( providerMapping.containsKey(engineName) ) {
					continue;
				}
				List<String> extensions = scriptEngineFactory.getExtensions();
				for (String  key : extensions) {
					if( providerMapping.containsKey(key) ) {
						continue label1;
					}
				}
				try {
					providerMapping.put(engineName, new ExpressFactoryProvider4jsr223( extensions.size() < 1 ? engineName : extensions.get(0)));
				}catch(Throwable ex) {
					logger.warn(ex.getMessage());
				}
			}
			}
		}finally {
			currentThread.setContextClassLoader(oldClassLoader);
		}
		

		String defaultEl = System.getProperty("arthas_default_el", "ognl");
		DEFAULT_EL = defaultEl;
		ExpressFactoryProvider defaultProvider = providerMapping.get(defaultEl);
		if( defaultProvider == null ) {
			if( "ognl".equals(defaultEl)  ) {
				defaultProvider = new ExpressFactoryProvider4ognl();
				providerMapping.put(defaultProvider.getPreifx(), defaultProvider);
			}else {
				defaultProvider = providerMapping.entrySet().iterator().next().getValue();
			}
		}
		this.defaultProvider = defaultProvider;
		//EL_PREFIX_PATTERN =  Pattern.compile( '^'+PROVIDER_MAPPING.keySet().toString().replace('[', '(').replaceAll("[ ,]+","|").replace(']', ')')+":{1,}");
		String elPrefixPattern = System.getProperty("el_prefix_pattern","^(js|groovy|mvel):{1,}");
		this.elPrefixPattern =  Pattern.compile(elPrefixPattern);
		logger.info("EL defaultProvider:"+defaultProvider);
		logger.info("EL_PREFIX_PATTERN:"+elPrefixPattern);
		logger.info("EL provider map:"+providerMapping);
	}
	
    }
    private static final ThreadLocal<Express> expressRef = new ThreadLocal<Express>() {
        @Override
        protected Express initialValue() {
            return getInst().defaultProvider.createExpress(null);
        }
    };
    
    public static ExpressFactoryProvider getExpressFactoryProvider(final String prefix) {
    	InstHolder inst1 = getInst();
    	if( prefix == null ) {
    		return  inst1.defaultProvider;
    	}
    	ExpressFactoryProvider provider =  inst1.providerMapping.get(prefix);
    	if( !JSR_223_DEFAULT_ON ) {
    		try {
    			provider = new ExpressFactoryProvider4jsr223(prefix);
    			inst.providerMapping.put(prefix, provider);
			}catch(Throwable ex) {
				logger.warn(ex.getMessage());
			}
    	}
    	return provider;
    }
    public static Object  evalExpress(ClassLoader classloader,String express,ExpressFactoryProvider defaultProvider ) throws ExpressException {
    	String prefix = null;
		final Matcher matcher = getInst().elPrefixPattern.matcher(express);
    	if( matcher.find() ) {
    		prefix = matcher.group(1);
    		express = express.substring(matcher.end());
    	}
    	ExpressFactoryProvider p = prefix == null  && defaultProvider != null ? defaultProvider :  getExpressFactoryProvider(prefix);
    	return p.createExpress(classloader).get(express);
    }
    
    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static Express threadLocalExpress(Object object) {
        return expressRef.get().reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        return getInst().defaultProvider.createExpress(classloader);
    }
}