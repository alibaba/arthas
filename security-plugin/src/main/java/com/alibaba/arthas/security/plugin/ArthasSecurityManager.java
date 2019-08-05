package com.alibaba.arthas.security.plugin;

import java.io.FileDescriptor;
import java.security.Permission;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

public class ArthasSecurityManager extends SecurityManager {

	private Logger logger = LoggerFactory.getLogger(ArthasSecurityManager.class);
	private SecurityManager delegate;

	public ArthasSecurityManager(SecurityManager securityManager) {
		this.delegate = securityManager;
	}

	@Override
	public void checkPermission(Permission perm) {
		logger.info("checkPermission, perm: {}", perm);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkPermission(perm);
	}

	@Override
	public void checkPermission(Permission perm, Object context) {
		logger.info("checkPermission, perm: {}", perm);
		if (this.delegate == null) {
			return;
		}

		this.delegate.checkPermission(perm, context);
	}

	@Override
	public void checkCreateClassLoader() {
		logger.info("checkCreateClassLoader");
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkCreateClassLoader();
	}

	@Override
	public void checkAccess(Thread t) {
		logger.info("checkAccess, thread: {}", t);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkAccess(t);
	}

	@Override
	public void checkAccess(ThreadGroup g) {
		logger.info("checkAccess, ThreadGroup: {}", g);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkAccess(g);
	}

	@Override
	public void checkExit(int status) {
		logger.info("checkExit, status: {}", status);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkExit(status);
	}

	@Override
	public void checkExec(String cmd) {
		logger.info("checkExec, cmd: {}", cmd);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkExec(cmd);
	}

	@Override
	public void checkLink(String lib) {
		logger.info("checkLink, checkLink: {}", lib);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkLink(lib);
	}

	@Override
	public void checkRead(FileDescriptor fd) {
		logger.info("checkRead, fd: {}", fd);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkRead(fd);
	}

	@Override
	public void checkRead(String file) {
		logger.info("checkRead, file: {}", file);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkRead(file);
	}

	@Override
	public void checkRead(String file, Object context) {
		logger.info("checkRead, file: {}", file);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkRead(file, context);
	}

	@Override
	public void checkWrite(FileDescriptor fd) {
		logger.info("checkWrite, fd: {}", fd);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkWrite(fd);
	}

	@Override
	public void checkWrite(String file) {
		logger.info("checkWrite, file: {}", file);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkWrite(file);
	}

	@Override
	public void checkDelete(String file) {
		logger.info("checkDelete, file: {}", file);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkDelete(file);
	}

	@Override
	public void checkConnect(String host, int port) {
		logger.info("checkConnect, host: {}, port: {}", host, port);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkConnect(host, port);
	}

	@Override
	public void checkConnect(String host, int port, Object context) {
		logger.info("checkConnect, host: {}, port: {}", host, port);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkConnect(host, port, context);
	}

	@Override
	public void checkListen(int port) {
		logger.info("checkListen, port: {}", port);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkListen(port);
	}

	@Override
	public void checkAccept(String host, int port) {
		logger.info("checkAccept, host: {}, port: {}", host, port);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkAccept(host, port);
	}

	@Override
	public void checkPropertiesAccess() {
		logger.info("checkPropertiesAccess");
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkPropertiesAccess();
	}

	@Override
	public void checkPropertyAccess(String key) {
		logger.info("checkPropertyAccess, key: {}", key);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkPropertyAccess(key);
	}

	@Override
	public void checkPrintJobAccess() {
		logger.info("checkPrintJobAccess");
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkPrintJobAccess();
	}

	@Override
	public void checkPackageAccess(String pkg) {
		logger.info("checkPackageAccess, pkg: {}", pkg);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkPackageAccess(pkg);
	}

	@Override
	public void checkPackageDefinition(String pkg) {
		logger.info("checkPackageDefinition, pkg: {}", pkg);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkPackageDefinition(pkg);
	}

	@Override
	public void checkSetFactory() {
		logger.info("checkSetFactory");
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkSetFactory();
	}

	@Override
	public void checkSecurityAccess(String target) {
		logger.info("checkSecurityAccess, target: {}", target);
		if (this.delegate == null) {
			return;
		}
		this.delegate.checkSecurityAccess(target);
	}

}
