package com.taobao.arthas.core.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ShellServerOptionsTest {

    @Test
    void shouldDefaultSessionTimeoutToThreeHours() {
        assertThat(ShellServerOptions.DEFAULT_SESSION_TIMEOUT).isEqualTo(TimeUnit.HOURS.toMillis(3));
        assertThat(new ShellServerOptions().getSessionTimeout()).isEqualTo(TimeUnit.HOURS.toMillis(3));
    }
}
