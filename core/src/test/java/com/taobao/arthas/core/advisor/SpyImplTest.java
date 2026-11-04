package com.taobao.arthas.core.advisor;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.taobao.arthas.core.util.StringUtils;

/**
 * 
 * @author hengyunabc 2021-07-14
 *
 */
public class SpyImplTest {

    @Test
    public void testSplitMethodInfo() throws Throwable {
        Assertions.assertThat(StringUtils.splitMethodInfo("a|b")).containsExactly("a", "b");
        Assertions.assertThat(StringUtils.splitMethodInfo("xxxxxxxxxx|fffffffffff")).containsExactly("xxxxxxxxxx",
                "fffffffffff");
        Assertions.assertThat(StringUtils.splitMethodInfo("print|(ILjava/util/List;)V")).containsExactly("print",
                "(ILjava/util/List;)V");
    }

    @Test
    public void testSplitInvokeInfo() throws Throwable {
        Assertions.assertThat(StringUtils.splitInvokeInfo("demo/MathGame|primeFactors|(I)Ljava/util/List;|24"))
                .containsExactly("demo/MathGame", "primeFactors", "(I)Ljava/util/List;", "24");

    }
}
