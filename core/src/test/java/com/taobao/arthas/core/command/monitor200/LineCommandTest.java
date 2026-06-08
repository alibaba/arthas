package com.taobao.arthas.core.command.monitor200;

import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;

public class LineCommandTest {

    @Test
    public void parseLinesShouldSupportCommaAndRepeatedOptions() {
        Assertions.assertThat(LineCommand.parseLines(Arrays.asList("51,56", "60", "56")))
                .containsExactly(51, 56, 60);
    }

    @Test
    public void parseLinesShouldRejectRangeSyntax() {
        Assertions.assertThatThrownBy(() -> LineCommand.parseLines(Collections.singletonList("51-60")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line range syntax is not supported");
    }

    @Test
    public void parseLinesShouldRejectInvalidNumbers() {
        Assertions.assertThatThrownBy(() -> LineCommand.parseLines(Arrays.asList("0", "abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line number must be greater than 0");
    }

    @Test
    public void lineAdviceShouldExposeLocalVarMapAndLocalsAlias() {
        Advice advice = Advice.newForLine(getClass().getClassLoader(), LineCommandTest.class,
                new ArthasMethod(LineCommandTest.class, "parseLinesShouldSupportCommaAndRepeatedOptions", "()V"),
                this, new Object[] { "arg" }, 123, new String[] { "input" },
                new Object[] { this, 7, "arthas" }, new String[] { "this", "count", "name" });

        Assertions.assertThat(advice.isLine()).isTrue();
        Assertions.assertThat(advice.getLineNumber()).isEqualTo(123);
        Assertions.assertThat(advice.getLocals()).containsExactly(7, "arthas");
        Assertions.assertThat(advice.getLocalVarMap()).containsEntry("count", 7).containsEntry("name", "arthas");
        Assertions.assertThat(advice.getLocalVarMap()).doesNotContainKey("this");
    }
}
