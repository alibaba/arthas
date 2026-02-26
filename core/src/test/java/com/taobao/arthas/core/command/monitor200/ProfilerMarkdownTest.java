package com.taobao.arthas.core.command.monitor200;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ProfilerMarkdownTest {

    @Test
    void shouldGenerateMarkdownWithHotspotsAndStacks() {
        // collapsed stacktraces: stack<space>count
        String collapsed = ""
                + "a.A.foo;java.lang.Thread.run 5\n"
                + "a.A.foo;java.lang.Thread.run 3\n"
                + "b.B.bar 2\n";

        String md = ProfilerMarkdown.toMarkdown(new ProfilerMarkdown.Options()
                .action("stop")
                .event("cpu")
                .threads(false)
                .topN(10)
                .collapsed(collapsed));

        Assertions.assertThat(md).contains("# Arthas profiler report (Markdown)");
        Assertions.assertThat(md).contains("total samples: 10");
        // top frame 统计：两条栈顶都是 java.lang.Thread.run (5+3)，另一条是 b.B.bar(2)
        Assertions.assertThat(md).contains("java.lang.Thread.run");
        Assertions.assertThat(md).contains("b.B.bar");
        Assertions.assertThat(md).contains("## Top 10 stacks");
    }

    @Test
    void shouldSkipThreadFrameWhenThreadsEnabled() {
        String collapsed = ""
                + "a.A.foo;java.lang.Thread.run 5\n"
                + "b.B.bar;[tid=1234] \"http-nio-8080-exec-1\" 3\n";

        String md = ProfilerMarkdown.toMarkdown(new ProfilerMarkdown.Options()
                .action("stop")
                .event("cpu")
                .threads(true)
                .topN(10)
                .collapsed(collapsed));

        // threads 模式下，栈顶线程帧应尽量被过滤，热点回退到上一帧
        Assertions.assertThat(md).contains("a.A.foo");
        Assertions.assertThat(md).contains("b.B.bar");
    }
}

