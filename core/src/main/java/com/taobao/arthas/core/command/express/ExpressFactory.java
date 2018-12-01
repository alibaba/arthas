package com.taobao.arthas.core.command.express;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final ExpressFactoryProvider PROVIDER ;
	private static final Map<String,ExpressFactoryProvider> PROVIDER_MAPPING = new HashMap();
	private static final Pattern EL_PREFIX_PATTERN ;
	 public static final String DEFAULT_EL;
	static {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader oldClassLoader = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(ExpressFactory.class.getClassLoader());
			Iterator<ExpressFactoryProvider> iterator = ServiceLoader.load(ExpressFactoryProvider.class,ExpressFactory.class.getClassLoader()).iterator();
			while(iterator.hasNext()){
				try {
					ExpressFactoryProvider p = iterator.next();
					PROVIDER_MAPPING.put(p.getPreifx(), p);
				}catch(Throwable ex) {
					logger.warn(ex.getMessage());
				}
			}
		
			List<ScriptEngineFactory> engineFactories = ExpressFactoryProvider4jsr223.manager.getEngineFactories();
			label1:
			for (ScriptEngineFactory scriptEngineFactory : engineFactories) {
				String engineName = scriptEngineFactory.getLanguageName();
				if( PROVIDER_MAPPING.containsKey(engineName) ) {
					continue;
				}
				List<String> extensions = scriptEngineFactory.getExtensions();
				for (String  key : extensions) {
					if( PROVIDER_MAPPING.containsKey(key) ) {
						continue label1;
					}
				}
				try {
					PROVIDER_MAPPING.put(engineName, new ExpressFactoryProvider4jsr223( extensions.size() < 1 ? engineName : extensions.get(0)));
				}catch(Throwable ex) {
					logger.warn(ex.getMessage());
				}
			}
		}finally {
			currentThread.setContextClassLoader(oldClassLoader);
		}
		String defaultEl = System.getProperty("arthas_default_el", "ognl");
		DEFAULT_EL = defaultEl;
		ExpressFactoryProvider defaultProvider = PROVIDER_MAPPING.get(defaultEl);
		if( defaultProvider == null ) {
			if( "ognl".equals(defaultEl)  ) {
				defaultProvider = new ExpressFactoryProvider4ognl();
				PROVIDER_MAPPING.put(defaultProvider.getPreifx(), defaultProvider);
			}else {
				defaultProvider = PROVIDER_MAPPING.entrySet().iterator().next().getValue();
			}
		}
		PROVIDER = defaultProvider;
		EL_PREFIX_PATTERN =  Pattern.compile( '^'+PROVIDER_MAPPING.keySet().toString().replace('[', '(').replaceAll("[ ,]+","|").replace(']', ')')+":{1,}");
		logger.info("EL defaultProvider:"+PROVIDER);
		logger.info("EL_PREFIX_PATTERN:"+EL_PREFIX_PATTERN);
		logger.info("EL provider map:"+PROVIDER_MAPPING);
	}
	
    private static final ThreadLocal<Express> expressRef = new ThreadLocal<Express>() {
        @Override
        protected Express initialValue() {
            return PROVIDER.createExpress(null);
        }
    };
    
    public static ExpressFactoryProvider getExpressFactoryProvider(final String prefix) {
    	return prefix ==  null ? PROVIDER : PROVIDER_MAPPING.get(prefix);
    }
    public static Object  evalExpress(ClassLoader classloader,String express,ExpressFactoryProvider defaultProvider ) throws ExpressException {
    	String prefix = null;
		final Matcher matcher = EL_PREFIX_PATTERN.matcher(express);
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
        return PROVIDER.createExpress(classloader);
    }
}