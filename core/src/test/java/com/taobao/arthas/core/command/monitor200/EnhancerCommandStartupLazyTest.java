package com.taobao.arthas.core.command.monitor200;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.session.impl.SessionImpl;

public class EnhancerCommandStartupLazyTest {
    @Test
    public void startupSessionShouldEnableLazyModeImplicitly() {
        WatchCommand command = new WatchCommand();
        Session session = new SessionImpl();

        assertThat(command.isLazy()).isFalse();
        assertThat(command.isLazy(session)).isFalse();

        session.put(Session.STARTUP_COMMAND, Boolean.TRUE);

        assertThat(command.isLazy(session)).isTrue();
        assertThat(command.isLazy()).isFalse();
    }

    @Test
    public void explicitLazyShouldStillWorkForNormalSession() {
        WatchCommand command = new WatchCommand();
        command.setLazy(true);

        assertThat(command.isLazy(new SessionImpl())).isTrue();
    }
}
