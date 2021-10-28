package com.taobao.arthas.core.security;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.util.StringUtils;

/**
 * TODO 支持不同角色不同权限，command按角色分类？
 * 
 * @author hengyunabc 2021-03-03
 *
 */
public class SecurityAuthenticatorImpl implements SecurityAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuthenticatorImpl.class);
    private String username;
    private String password;
    private Subject subject;

    public SecurityAuthenticatorImpl(String username, String password) {
        if (username != null && password == null) {
            password = StringUtils.randomString(32);
            logger.info("\nUsing generated security password: {}\n", password);
        }
        if (username == null && password != null) {
            username = ArthasConstants.DEFAULT_USERNAME;
        }

        this.username = username;
        this.password = password;

        subject = new Subject();
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRoleClassNames(String names) {
        // TODO Auto-generated method stub

    }

    @Override
    public Subject login(Principal principal) throws LoginException {
        if (principal == null) {
            return null;
        }
        if (principal instanceof BasicPrincipal) {
            BasicPrincipal basicPrincipal = (BasicPrincipal) principal;
            if (basicPrincipal.getName().equals(username) && basicPrincipal.getPassword().equals(this.password)) {
                return subject;
            }
        }
        if (principal instanceof LocalConnectionPrincipal) {
            return subject;
        }

        return null;
    }

    @Override
    public void logout(Subject subject) throws LoginException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getUserRoles(Subject subject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean needLogin() {
        return username != null && password != null;
    }

}
