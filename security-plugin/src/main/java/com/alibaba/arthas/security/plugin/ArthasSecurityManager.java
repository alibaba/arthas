package com.alibaba.arthas.security.plugin;

import java.io.FileDescriptor;
import java.security.Permission;

import com.alibaba.arthas.deps.org.slf4j.Logger;

public class ArthasSecurityManager extends SecurityManager {

	private Logger logger;
	private SecurityManager delegate;

	public ArthasSecurityManager(SecurityManager securityManager) {
		this.delegate = securityManager;
	}

	@Override
	public void checkPermission(Permission perm) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkPermission, perm: {}", perm);
		this.delegate.checkPermission(perm);
	}

	@Override
	public void checkPermission(Permission perm, Object context) {
		if (this.delegate == null) {
			return;
		}

		logger.info("checkPermission, perm: {}", perm);
		this.delegate.checkPermission(perm, context);
	}

	@Override
	public void checkCreateClassLoader() {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkCreateClassLoader");
		this.delegate.checkCreateClassLoader();
	}

	@Override
	public void checkAccess(Thread t) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkAccess, thread: {}", t);
		this.delegate.checkAccess(t);
	}

	@Override
	public void checkAccess(ThreadGroup g) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkAccess, ThreadGroup: {}", g);
		this.delegate.checkAccess(g);
	}

	@Override
	public void checkExit(int status) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkExit, status: {}", status);
		this.delegate.checkExit(status);
	}

	@Override
	public void checkExec(String cmd) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkExec, cmd: {}", cmd);
		this.delegate.checkExec(cmd);
	}

	@Override
	public void checkLink(String lib) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkLink, checkLink: {}", lib);
		this.delegate.checkLink(lib);
	}

	@Override
	public void checkRead(FileDescriptor fd) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkRead, fd: {}", fd);
		this.delegate.checkRead(fd);
	}

	@Override
	public void checkRead(String file) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkRead, file: {}", file);
		this.delegate.checkRead(file);
	}

	@Override
	public void checkRead(String file, Object context) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkRead, file: {}", file);
		this.delegate.checkRead(file, context);
	}

	@Override
	public void checkWrite(FileDescriptor fd) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkWrite, fd: {}", fd);
		this.delegate.checkWrite(fd);
	}

	@Override
	public void checkWrite(String file) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkWrite, file: {}", file);
		this.delegate.checkWrite(file);
	}

	@Override
	public void checkDelete(String file) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkDelete, file: {}", file);
		this.delegate.checkDelete(file);
	}

	@Override
	public void checkConnect(String host, int port) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkConnect, host: {}, port: {}", host, port);
		this.delegate.checkConnect(host, port);
	}

	@Override
	public void checkConnect(String host, int port, Object context) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkConnect, host: {}, port: {}", host, port);
		this.delegate.checkConnect(host, port, context);
	}

	@Override
	public void checkListen(int port) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkListen, port: {}", port);
		this.delegate.checkListen(port);
	}

	@Override
	public void checkAccept(String host, int port) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkAccept, host: {}, port: {}", host, port);
		this.delegate.checkAccept(host, port);
	}

	@Override
	public void checkPropertiesAccess() {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkPropertiesAccess");
		this.delegate.checkPropertiesAccess();
	}

	@Override
	public void checkPropertyAccess(String key) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkPropertyAccess, key: {}", key);
		this.delegate.checkPropertyAccess(key);
	}

	@Override
	public void checkPrintJobAccess() {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkPrintJobAccess");
		this.delegate.checkPrintJobAccess();
	}

	@Override
	public void checkPackageAccess(String pkg) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkPackageAccess, pkg: {}", pkg);
		this.delegate.checkPackageAccess(pkg);
	}

	@Override
	public void checkPackageDefinition(String pkg) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkPackageDefinition, pkg: {}", pkg);
		this.delegate.checkPackageDefinition(pkg);
	}

	@Override
	public void checkSetFactory() {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkSetFactory");
		this.delegate.checkSetFactory();
	}

	@Override
	public void checkSecurityAccess(String target) {
		if (this.delegate == null) {
			return;
		}
		logger.info("checkSecurityAccess, target: {}", target);
		this.delegate.checkSecurityAccess(target);
	}

}
