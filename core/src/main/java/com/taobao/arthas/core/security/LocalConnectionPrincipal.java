package com.taobao.arthas.core.security;

import java.security.Principal;

/**
 * 本地连接的特殊处理 {@link Principal}.
 * 
 * @author hengyunabc 2021-09-01
 */
public final class LocalConnectionPrincipal implements Principal {

    public LocalConnectionPrincipal() {
    }

    @Override
    public String getName() {
        return null;
    }

    public String getUsername() {
        return null;
    }

    public String getPassword() {
        return null;
    }
}