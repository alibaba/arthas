package com.taobao.arthas.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ognl.OgnlRuntime;

class GlobalOptionsTest {

    @Test
    void test() {
        GlobalOptions.updateOnglStrict(true);
        Assertions.assertThat(OgnlRuntime.getUseStricterInvocationValue()).isTrue();
        GlobalOptions.updateOnglStrict(false);
        Assertions.assertThat(OgnlRuntime.getUseStricterInvocationValue()).isFalse();
    }

}
