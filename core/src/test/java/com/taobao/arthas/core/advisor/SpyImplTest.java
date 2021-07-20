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
    }

    @Test
    public void testSplitInvokeInfo() throws Throwable {
        Assertions.assertThat(StringUtils.splitInvokeInfo("a|b|c")).containsExactly("a", "b", "c");
        Assertions.assertThat(StringUtils.splitInvokeInfo("xxxxxxxxxx|fffffffffff|yyy")).containsExactly("xxxxxxxxxx",
                "fffffffffff", "yyy");
    }
}
