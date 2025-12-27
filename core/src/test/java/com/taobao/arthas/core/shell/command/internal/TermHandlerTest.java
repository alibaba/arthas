package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.shell.term.Term;
import io.netty.util.internal.InternalThreadLocalMap;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TermHandlerTest {

    @Test
    public void testShouldRemoveNettyInternalThreadLocalMapAfterWrite() {
        InternalThreadLocalMap.remove();

        Term term = Mockito.mock(Term.class);
        Mockito.when(term.write(Mockito.anyString())).thenAnswer(invocation -> {
            InternalThreadLocalMap.get();
            return term;
        });

        new TermHandler(term).apply("hello\n");

        Assert.assertNull(InternalThreadLocalMap.getIfSet());
    }

    @Test
    public void testShouldRemoveNettyInternalThreadLocalMapWhenWriteThrows() {
        InternalThreadLocalMap.remove();

        Term term = Mockito.mock(Term.class);
        Mockito.when(term.write(Mockito.anyString())).thenAnswer(invocation -> {
            InternalThreadLocalMap.get();
            throw new RuntimeException("boom");
        });

        try {
            new TermHandler(term).apply("hello\n");
            Assert.fail("should throw");
        } catch (RuntimeException e) {
            Assert.assertEquals("boom", e.getMessage());
        }

        Assert.assertNull(InternalThreadLocalMap.getIfSet());
    }
}

