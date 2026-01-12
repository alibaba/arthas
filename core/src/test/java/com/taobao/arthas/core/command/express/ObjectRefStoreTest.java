package com.taobao.arthas.core.command.express;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ObjectRefStoreTest {

    @Test
    public void testPutGetRemoveWithNamespace() throws ExpressException {
        ObjectRefStore.ref().clearAll();

        Object value = new Object();

        Express express1 = ExpressFactory.unpooledExpress(ObjectRefStoreTest.class.getClassLoader());
        express1.bind("v", value).bind(new Object());
        Object putResult = express1.get("#ref.ns(\"ns1\").put(\"k\", #v)");
        Assert.assertSame(value, putResult);

        Express express2 = ExpressFactory.unpooledExpress(ObjectRefStoreTest.class.getClassLoader());
        express2.bind(new Object());
        Object getResult = express2.get("#ref.ns(\"ns1\").get(\"k\")");
        Assert.assertSame(value, getResult);

        Object removeResult = express2.get("#ref.ns(\"ns1\").remove(\"k\")");
        Assert.assertSame(value, removeResult);

        Object afterRemove = express2.get("#ref.ns(\"ns1\").get(\"k\")");
        Assert.assertNull(afterRemove);
    }

    @Test
    public void testNamespaceIsolation() throws ExpressException {
        ObjectRefStore.ref().clearAll();

        Object valueA = new Object();
        Object valueB = new Object();

        Express express = ExpressFactory.unpooledExpress(ObjectRefStoreTest.class.getClassLoader());
        express.bind("a", valueA).bind("b", valueB).bind(new Object());

        Assert.assertSame(valueA, express.get("#ref.ns(\"a\").put(\"k\", #a)"));
        Assert.assertSame(valueB, express.get("#ref.ns(\"b\").put(\"k\", #b)"));

        Assert.assertSame(valueA, express.get("#ref.ns(\"a\").get(\"k\")"));
        Assert.assertSame(valueB, express.get("#ref.ns(\"b\").get(\"k\")"));
        Assert.assertNull(express.get("#ref.ns(\"c\").get(\"k\")"));
    }

    @Test
    public void testLsAndNamespaces() throws ExpressException {
        ObjectRefStore.ref().clearAll();

        Object value = new Object();

        Express express = ExpressFactory.unpooledExpress(ObjectRefStoreTest.class.getClassLoader());
        express.bind("v", value).bind(new Object());
        express.get("#ref.ns(\"ns-list\").put(\"k\", #v)");

        List<?> ls = (List<?>) express.get("#ref.ns(\"ns-list\").ls()");
        Assert.assertEquals(1, ls.size());

        Map<?, ?> item = (Map<?, ?>) ls.get(0);
        Assert.assertEquals("ns-list", item.get("namespace"));
        Assert.assertEquals("k", item.get("name"));

        List<?> namespaces = (List<?>) express.get("#ref.namespaces()");
        Assert.assertTrue(namespaces.contains("ns-list"));
    }
}

