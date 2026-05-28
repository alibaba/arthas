package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.advisor.Advice;
import ognl.OgnlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArthasObjectPropertyAccessorTest {

    private Express express;

    @BeforeEach
    public void setUp () {
        Fetcher fetcher = new Fetcher().add(new Fetcher.Fetch()
                .add(new FlowContext("aa"))
                .add(new FlowContext("bb"))
        ).add(new Fetcher.Fetch()
                .add(new FlowContext("cc"))
                .add(new FlowContext("dd"))
                .add(new FlowContext("ee"))
        );

        Object[] params = new Object[4];
        params[0] = fetcher;
        Advice advice = Advice.newForAfterReturning(null, getClass(), null, null, params, null);
        express = ExpressFactory.unpooledExpress(null).bind(advice);
    }

    @Test
    void getPossibleProperty() throws ExpressException {
        assertInstanceOf(List.class, express.get("params[0].completedFetches"));
        assertEquals(2, ((List<?>) express.get("params[0].completedFetches")).size());
        assertThrows(ExpressException.class, () -> express.is("params[0].hasCompletedFetches"));
        assertTrue(express.is("params[0].hasCompletedFetches()"));
        assertThrows(ExpressException.class, () -> express.is("params[0].getCompletedFetches"));
        assertTrue(express.is("params[0].getCompletedFetches()"));
        assertInstanceOf(Fetcher.Fetch.class, express.get("params[0].completedFetches[1]"));
        assertInstanceOf(List.class, express.get("params[0].completedFetches[1].flowContexts"));
        assertEquals(3, ((List) express.get("params[0].completedFetches[1].flowContexts")).size());
        assertTrue(express.is("params[0].completedFetches[1].hasFlowContexts()"));
        assertTrue(express.is("params[0].completedFetches[1].getFlowContexts()"));
        assertInstanceOf(List.class, express.get("params[0].completedFetches[1].getFlowContexts1()"));
        assertInstanceOf(List.class, express.get("params[0].completedFetches.{flowContexts.{flowAttribute.bxApp}}"));
        assertIterableEquals(Arrays.asList(
                Arrays.asList("aa", "bb"),
                Arrays.asList("cc", "dd", "ee")
        ), (Iterable<?>) express.get("params[0].completedFetches.{flowContexts.{flowAttribute.bxApp}}"));
    }



}