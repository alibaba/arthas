package com.alibaba.arthas.security.plugin;

import com.alibaba.arthas.plugin.PluginActivator;
import com.alibaba.arthas.plugin.PluginContext;

/**
 * 1. 跟踪所有的 Runtime exec
 * 获取所有的 servlet 请求的
 * @author hengyunabc 2019-04-04
 *
 */
public class SecurityPluginActivator implements PluginActivator{

	@Override
	public boolean enabled(PluginContext context) {
		return true;
	}

	@Override
	public void init(PluginContext context) throws Exception {
		try {
			SecurityManager securityManager = System.getSecurityManager();

			securityManager = new ArthasSecurityManager(securityManager);
			System.setSecurityManager(securityManager);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(PluginContext context) throws Exception {

	}

	@Override
	public void stop(PluginContext context) throws Exception {

	}

}
