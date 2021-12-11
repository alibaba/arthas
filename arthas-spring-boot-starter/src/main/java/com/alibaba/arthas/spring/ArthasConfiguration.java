package com.alibaba.arthas.spring;

import com.taobao.arthas.agent.attach.ArthasAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.env.ConfigurableEnvironment;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hengyunabc 2020-06-22
 *
 */
@ConditionalOnProperty(name = "spring.arthas.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ ArthasProperties.class })
public class ArthasConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(ArthasConfiguration.class);

	@Autowired
	ConfigurableEnvironment environment;

	/**
	 * <pre>
	 * 1. 提取所有以 arthas.* 开头的配置项，再统一转换为Arthas配置
	 * 2. 避免某些配置在新版本里支持，但在ArthasProperties里没有配置的情况。
	 * </pre>
	 */
	@ConfigurationProperties(prefix = "arthas")
	@ConditionalOnMissingBean(name="arthasConfigMap")
	@Bean
	public HashMap<String, String> arthasConfigMap() {
		return new HashMap<String, String>();
	}

	@ConditionalOnMissingBean
	@Bean
	public ArthasAgent arthasAgent(@Autowired @Qualifier("arthasConfigMap") Map<String, String> arthasConfigMap,
			@Autowired ArthasProperties arthasProperties) throws Throwable {
        arthasConfigMap = StringUtils.removeDashKey(arthasConfigMap);
        ArthasProperties.updateArthasConfigMapDefaultValue(arthasConfigMap);
        /**
         * @see org.springframework.boot.context.ContextIdApplicationContextInitializer#getApplicationId(ConfigurableEnvironment)
         */
        String appName = environment.getProperty("spring.application.name");
        if (arthasConfigMap.get("appName") == null && appName != null) {
            arthasConfigMap.put("appName", appName);
        }

		// 给配置全加上前缀
		Map<String, String> mapWithPrefix = new HashMap<String, String>(arthasConfigMap.size());
		for (Entry<String, String> entry : arthasConfigMap.entrySet()) {
			mapWithPrefix.put("arthas." + entry.getKey(), entry.getValue());
		}

		final ArthasAgent arthasAgent = new ArthasAgent(mapWithPrefix, arthasProperties.getHome(),false, null);
		try {
			//直接调用arthasAgent.init()
			if (tryInit(arthasAgent, ()->{},true)) {
				return arthasAgent;
			}
			//调用arthasAgent.init()前先调用System.gc()
			if (tryInit(arthasAgent, this::triggerSystemGc,true)) {
				return arthasAgent;
			}
			//调用arthasAgent.init()前先调用removeAttachName()
			if (tryInit(arthasAgent, this::removeAttachName,false)) {
				return arthasAgent;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			if (!arthasProperties.isSlientInit()) {
				throw e;
			}
		}

		return arthasAgent;

	}
	/**
	 * 尝试调用com.taobao.arthas.agent.attach.ArthasAgent#init()方法，并在调用之前执行java.lang.Runnable#run()
	 *
	 * @param arthasAgent arthasAgent.init();
	 * @param runBeforeInit runBeforeInit.run();
     * @param ignoreException 当调用init异常时，是否忽略
     */
	private boolean tryInit(ArthasAgent arthasAgent, Runnable runBeforeInit,boolean ignoreException) {
		try {
			runBeforeInit.run();
			arthasAgent.init();
			logger.info("Arthas agent start success.");
			return true;
		} catch (Throwable e) {
			if (!isLoadedInAnotherClassLoader(e) || !ignoreException) {
				throw e;
			}
			return false;
		}
	}

    /**
     * 检测异常是否为UnsatisfiedLinkError实例，并且错误消息后缀为： System.mapLibraryName("attach") + " loaded in another classloader"
     */
    private boolean isLoadedInAnotherClassLoader(Throwable e) {
        Throwable rootCause = NestedExceptionUtils.getRootCause(e);
        if (rootCause==null
                || !(rootCause instanceof UnsatisfiedLinkError)
                || (rootCause.getMessage() == null)) {
            return false;
        }
        String libname = System.mapLibraryName("attach");
        String expectedErrorMessage = libname + " loaded in another classloader";
        return rootCause.getMessage().endsWith(expectedErrorMessage);
    }

	private void triggerSystemGc() {
		logger.info("attach失败，尝试手工触发gc");
		System.gc();
	}
	/**
	 * 通过反射移除java.lang.ClassLoader#loadedLibraryNames里面的包含attach名称的值
	 */
	private void removeAttachName() {
		String name = System.mapLibraryName("attach");
		try {
			Field loadedLibraryNames = ClassLoader.class.getDeclaredField("loadedLibraryNames");
			loadedLibraryNames.setAccessible(true);
			Collection<String> libnames = (Collection<String>) loadedLibraryNames.get(null);
			Iterator<String> iterator = libnames.iterator();
			while (iterator.hasNext()) {
				String libname = iterator.next();
				if (libname.endsWith(name)) {
					logger.info("attach失败，移除{}，解除jvm加载lib的限定：同一个lib只能被同一个ClassLoader类加载器加载",libname);
					iterator.remove();
					return;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
