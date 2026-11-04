package com.taobao.arthas.core.security;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * 
 * @author hengyunabc 2021-03-04
 *
 */
public class SecurityAuthenticatorImplTest {

    @Test
    public void test1() throws LoginException {
        String username = "test";
        String password = "ppp";
        SecurityAuthenticatorImpl auth = new SecurityAuthenticatorImpl(username, password);

        Assertions.assertThat(auth.needLogin()).isTrue();

        Principal principal = new BasicPrincipal(username, password);
        Subject subject = auth.login(principal);

        Assertions.assertThat(subject).isNotNull();
    }

    @Test
    public void test2() {
        String username = "test";
        String password = null;
        SecurityAuthenticatorImpl auth = new SecurityAuthenticatorImpl(username, password);
        Assertions.assertThat(auth.needLogin()).isTrue();
    }

}
