package com.taobao.arthas.core.security;

import java.security.Principal;

/**
 * Bearer Token {@link Principal}.
 */
public final class BearerPrincipal implements Principal {

    private final String token;

    public BearerPrincipal(String token) {
        this.token = token;
    }

    @Override
    public String getName() {
        return "bearer";
    }

    public String getToken() {
        return token;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((token == null) ? 0 : token.hashCode());
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
        BearerPrincipal other = (BearerPrincipal) obj;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }

    @Override
    public String toString() {
        // do not display the token for security reasons
        return "BearerPrincipal[***]";
    }
}
