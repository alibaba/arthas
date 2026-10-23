package com.taobao.arthas.core.command.monitor200;

import java.lang.reflect.Field;

import com.taobao.arthas.core.command.model.ProfilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import one.profiler.AsyncProfiler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfilerCommandTest {
    @Test
    void shouldKeepMeminfoActionWhileReturningNativeMetrics() throws Exception {
        AsyncProfiler profiler = mock(AsyncProfiler.class);
        String metrics = "mem_calltracestorage_kb 10244\nsamples_total 42\n";
        when(profiler.execute("metrics")).thenReturn(metrics);
        Field field = ProfilerCommand.class.getDeclaredField("profiler");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, profiler);
        try {
            CommandProcess process = mock(CommandProcess.class);
            ProfilerCommand command = new ProfilerCommand();
            command.setAction("meminfo");
            command.process(process);

            ArgumentCaptor<ProfilerModel> result = ArgumentCaptor.forClass(ProfilerModel.class);
            verify(process).appendResult(result.capture());
            assertEquals("meminfo", result.getValue().getAction());
            assertEquals(metrics, result.getValue().getExecuteResult());
            verify(profiler).execute("metrics");
            verify(profiler, never()).execute("meminfo");
            verify(process).end();
        } finally {
            field.set(null, previous);
        }
    }
}
