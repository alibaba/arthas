package com.taobao.arthas.core.security;

import java.security.Principal;

/**
 * Basic {@link Principal}.
 * 
 * @author hengyunabc 2021-03-04
 */
public final class BasicPrincipal implements Principal {

    private final String username;
    private final String password;

    public BasicPrincipal(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicPrincipal other = (BasicPrincipal) obj;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    @Override
    public String toString() {
        // do not display the password
        return "BasicPrincipal[" + username + "]";
    }
}