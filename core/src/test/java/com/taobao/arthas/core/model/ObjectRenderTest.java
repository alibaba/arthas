package com.taobao.arthas.core.model;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.object.ObjectInspector;
import com.taobao.arthas.core.util.object.ObjectRenderer;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gongdewei 2020/9/24
 */
public class ObjectRenderTest {

    static int arrayLenLimit = 15;
    static int objectNumLimit = 30;

    @Test
    public void testPrimitiveTypes() {

        testSimpleObject(Integer.valueOf(100), "@Integer[100]");
        testSimpleObject(Long.valueOf(1000L), "@Long[1000]");
        testSimpleObject(Short.valueOf((short) 100), "@Short[100]");
        testSimpleObject((byte) 100, "@Byte[100]");
        testSimpleObject('A', "@Character[A]");
        testSimpleObject(true, "@Boolean[true]");
        testSimpleObject(100.0f, "@Float[100.0]");
        testSimpleObject(100.0, "@Double[100.0]");

        testSimpleObject("中文abc", "@String[中文abc]");

    }

    @Test
    public void testCollections() {
        testSimpleList(Arrays.asList(1, 2, 3));

        testSimpleList(Arrays.asList((short) 1, (short) 2, (short) 3));

        testSimpleList(Arrays.asList(1L, 2L, 3L));

        testSimpleList(Arrays.asList((byte) 1, (byte) 2, (byte) 3));

        testSimpleList(Arrays.asList((char) 1, (char) 2, (char) 3));

        testSimpleList(Arrays.asList(1.0, 2.0, 3.0));

        testSimpleList(Arrays.asList(1.0F, 2.0F, 3.0F));

        testSimpleList(Arrays.asList("1", "2", "3"));

        List<AtomicInteger> atomicIntegers = Arrays.asList(new AtomicInteger(1), new AtomicInteger(2), new AtomicInteger(3));
        System.out.println("expand 1: ");
        testList(atomicIntegers, 1);
        System.out.println("expand 2: ");
        testList(atomicIntegers, 2);
    }

    @Test
    public void testArrays() {
        testPrimitiveArray(new int[]{1, 2, 3}, "{\"size\":3,\"type\":\"int[]\",\"value\":[1,2,3]}", "@int[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new short[]{1, 2, 3}, "{\"size\":3,\"type\":\"short[]\",\"value\":[1,2,3]}", "@short[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new byte[]{1, 2, 3}, "{\"size\":3,\"type\":\"byte[]\",\"value\":[1,2,3]}", "@byte[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new long[]{1, 2, 3}, "{\"size\":3,\"type\":\"long[]\",\"value\":[1,2,3]}", "@long[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new float[]{1, 2, 3}, "{\"size\":3,\"type\":\"float[]\",\"value\":[1.0,2.0,3.0]}", "@float[][\n" +
                "    1.0,\n" +
                "    2.0,\n" +
                "    3.0,\n" +
                "]");
        testPrimitiveArray(new double[]{1, 2, 3}, "{\"size\":3,\"type\":\"double[]\",\"value\":[1.0,2.0,3.0]}", "@double[][\n" +
                "    1.0,\n" +
                "    2.0,\n" +
                "    3.0,\n" +
                "]");

        testPrimitiveArray(new Integer[]{1, 2, 3}, "{\"size\":3,\"type\":\"Integer[]\",\"value\":[1,2,3]}", "@Integer[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new Short[]{1, 2, 3}, "{\"size\":3,\"type\":\"Short[]\",\"value\":[1,2,3]}", "@Short[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new Byte[]{1, 2, 3}, "{\"size\":3,\"type\":\"Byte[]\",\"value\":[1,2,3]}", "@Byte[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new Long[]{1L, 2L, 3L}, "{\"size\":3,\"type\":\"Long[]\",\"value\":[1,2,3]}", "@Long[][\n" +
                "    1,\n" +
                "    2,\n" +
                "    3,\n" +
                "]");
        testPrimitiveArray(new Float[]{1f, 2f, 3f}, "{\"size\":3,\"type\":\"Float[]\",\"value\":[1.0,2.0,3.0]}", "@Float[][\n" +
                "    1.0,\n" +
                "    2.0,\n" +
                "    3.0,\n" +
                "]");
        testPrimitiveArray(new Double[]{1.0, 2.0, 3.0}, "{\"size\":3,\"type\":\"Double[]\",\"value\":[1.0,2.0,3.0]}", "@Double[][\n" +
                "    1.0,\n" +
                "    2.0,\n" +
                "    3.0,\n" +
                "]");

        testPrimitiveArray(new int[20], "{\"size\":20,\"type\":\"int[]\",\"value\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}",
                "@int[][\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    0,\n" +
                        "    15 out of 20 displayed, 5 remaining.\n" +
                        "]");

//        testPrimitiveArray(new char[]{1, 2, 3}, new Object[]{"SOH", "STX", "ETX"});
//        testPrimitiveArray(new Character[]{1, 2, 3}, new Object[]{"SOH", "STX", "ETX"});
        testPrimitiveArray(new char[]{1, 2, 3, '\n', '\r'},
                "{\"size\":5,\"type\":\"char[]\",\"value\":[\"\\\\u0001\",\"\\\\u0002\",\"\\\\u0003\",\"\\\\u000A\",\"\\\\u000D\"]}",
                "@char[][\n" +
                        "    \\u0001,\n" +
                        "    \\u0002,\n" +
                        "    \\u0003,\n" +
                        "    \\u000A,\n" +
                        "    \\u000D,\n" +
                        "]");

        testPrimitiveArray(new Character[]{1, 2, 3, '\n', '\r'},
                "{\"size\":5,\"type\":\"Character[]\",\"value\":[\"\\\\u0001\",\"\\\\u0002\",\"\\\\u0003\",\"\\\\u000A\",\"\\\\u000D\"]}",
                "@Character[][\n" +
                        "    \\u0001,\n" +
                        "    \\u0002,\n" +
                        "    \\u0003,\n" +
                        "    \\u000A,\n" +
                        "    \\u000D,\n" +
                        "]");

        testArray(new Object[]{1.0, 2L, 3.0f}, 2);

        testArray(new String[]{"str1", "2", "3"}, 2);

        testArray(new Object[150], 2);

        //Object[]
        testArray(new Object[]{new AtomicInteger(1), new AtomicInteger(2), new AtomicInteger(3)}, 2);
        testArray(new AtomicInteger[]{new AtomicInteger(1), new AtomicInteger(2), new AtomicInteger(3)}, 2);

    }

    @Test
    public void testMaps() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").parse("2020-09-27 16:26:18.587 +0800");

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("strValue", "Arthas中国");
        map.put("intValue", 1234);
        map.put("boolValue", true);
        map.put("doubleValue", 123.0);
        map.put("floatValue", 123.0f);
        map.put("listValue", Arrays.asList(1,2,3));
        map.put("IntArray", new Integer[]{1,2,3});
        map.put("objectArray", new Object[]{1,2,3});
        map.put("dateValue", date);
        map.put("objectValue", new Object());
        map.put("nullValue", null);

        ObjectVO objectVO1 = inspectObject(map, 1);
        printObject(objectVO1);

        Assert.assertEquals("@LinkedHashMap[\n" +
                "    @String[strValue] : @String[Arthas中国],\n" +
                "    @String[intValue] : @Integer[1234],\n" +
                "    @String[boolValue] : @Boolean[true],\n" +
                "    @String[doubleValue] : @Double[123.0],\n" +
                "    @String[floatValue] : @Float[123.0],\n" +
                "    @String[listValue] : @ArrayList[isEmpty=false;size=3],\n" +
                "    @String[IntArray] : @Integer[][isEmpty=false;size=3],\n" +
                "    @String[objectArray] : @Object[][isEmpty=false;size=3],\n" +
                "    @String[dateValue] : @Date[2020-09-27 16:26:18.587 +0800],\n" +
                "    @String[objectValue] : @Object[java.lang.Object@ffffffff],\n" +
                "    @String[nullValue] : null,\n" +
                "]", replaceHashCode(ObjectRenderer.render(objectVO1)));


        ObjectVO objectVO2 = inspectObject(map, 2);
        printObject(objectVO2);
        Assert.assertEquals("@LinkedHashMap[\n" +
                "    @String[strValue] : @String[Arthas中国],\n" +
                "    @String[intValue] : @Integer[1234],\n" +
                "    @String[boolValue] : @Boolean[true],\n" +
                "    @String[doubleValue] : @Double[123.0],\n" +
                "    @String[floatValue] : @Float[123.0],\n" +
                "    @String[listValue] : @ArrayList[\n" +
                "        @Integer[1],\n" +
                "        @Integer[2],\n" +
                "        @Integer[3],\n" +
                "    ],\n" +
                "    @String[IntArray] : @Integer[][\n" +
                "        1,\n" +
                "        2,\n" +
                "        3,\n" +
                "    ],\n" +
                "    @String[objectArray] : @Object[][\n" +
                "        @Integer[1],\n" +
                "        @Integer[2],\n" +
                "        @Integer[3],\n" +
                "    ],\n" +
                "    @String[dateValue] : @Date[2020-09-27 16:26:18.587 +0800],\n" +
                "    @String[objectValue] : @Object[\n" +
                "    ],\n" +
                "    @String[nullValue] : null,\n" +
                "]", replaceHashCode(ObjectRenderer.render(objectVO2)));
    }

    private void printObject(ObjectVO objectVO2) {
        System.out.println(JSON.toJSONString(objectVO2));
        System.out.println();
        System.out.println(ObjectRenderer.render(objectVO2));
        System.out.println();
    }

    @Test
    public void testEnums() {
        ObjectVO objectVO = inspectObject(Thread.State.RUNNABLE, 1);
        printObject(objectVO);
        Assert.assertEquals(Thread.State.RUNNABLE.toString(), objectVO.getValue());

        ObjectVO objectVO2 = inspectObject(Thread.State.values(), 2);
        printObject(objectVO2);

    }

    @Test
    public void testDate() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").parse("2020-09-27 16:26:18.587 +0800");
        ObjectVO objectVO = inspectObject(date, 1);
        printObject(objectVO);
        Assert.assertEquals("@Date[2020-09-27 16:26:18.587 +0800]", objectVO.toString());

    }

    @Test
    public void testNestObject() {
        NestedClass nestedClass = new NestedClass(100);
        ObjectVO objectVO = inspectObject(nestedClass, 3);
        printObject(objectVO);

        Assert.assertEquals("@NestedClass[\n" +
                "    code=@Integer[100],\n" +
                "    c1=@NestedClass[\n" +
                "        code=@Integer[1],\n" +
                "        c1=@NestedClass[\n" +
                "            code=@Integer[1],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "        c2=@NestedClass[\n" +
                "            code=@Integer[2],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "    ],\n" +
                "    c2=@NestedClass[\n" +
                "        code=@Integer[2],\n" +
                "        c1=@NestedClass[\n" +
                "            code=@Integer[1],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "        c2=@NestedClass[\n" +
                "            code=@Integer[2],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "    ],\n" +
                "]", replaceHashCode(ObjectRenderer.render(objectVO)));
    }

    @Test
    public void testObjectTooLargeException() {
        NestedClass nestedClass = new NestedClass(100);
        ObjectInspector objectInspector = new ObjectInspector(10, arrayLenLimit);
        ObjectVO objectVO = objectInspector.inspect(nestedClass, 4);
        printObject(objectVO);

        Assert.assertTrue(replaceHashCode(ObjectRenderer.render(objectVO)).contains("@NestedClass[\n" +
                        "    code=@Integer[100],\n" +
                        "    c1=@NestedClass[\n" +
                        "        code=@Integer[1],\n" +
                        "        c1=@NestedClass[\n" +
                        "            code=@Integer[1],\n" +
                        "            c1=@NestedClass[\n" +
                        "                code=@Integer[1],\n" +
                        "                c1=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                        "                c2=@NestedClass[com.taobao.arthas.core.model.ObjectRenderTest$NestedClass@ffffffff],\n" +
                        "            ],\n" +
                        "            c2=@NestedClass[...],\n" +
                        "        ],\n" +
                        "        c2=@NestedClass[...],\n" +
                        "    ],\n" +
                        "    c2=@NestedClass[...],\n" +
                        "] Number of objects exceeds limit: 10"));
    }

    /**
     * 显示基类属性值
     */
    @Test
    public void testObjectViewBaseFieldValue() {
        SonBean sonBean = new SonBean();
        sonBean.setI(10);
        sonBean.setJ("test");

        ObjectVO objectVO = inspectObject(sonBean, 3);
        printObject(objectVO);

        Assert.assertTrue(ObjectRenderer.render(objectVO).contains("i=@Integer[10]"));
    }

    private static class BaseBean {
        private int i;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    private static class SonBean extends BaseBean {
        private String j;

        public String getJ() {
            return j;
        }

        public void setJ(String j) {
            this.j = j;
        }
    }

    private ObjectInspector newInspector() {
        return new ObjectInspector(objectNumLimit, arrayLenLimit);
    }

    private ObjectVO inspectObject(Object object, int expand) {
        return new ObjectInspector(objectNumLimit, arrayLenLimit).inspect(object, expand);
    }

    private void testPrimitiveArray(Object array, String testJson, String testString) {
        ObjectVO objectVO = inspectObject(array, 2);
        Assert.assertEquals(array.getClass().getSimpleName(), objectVO.getType());
        printObject(objectVO);

        Assert.assertEquals(testJson, JSON.toJSONString(objectVO));
        Assert.assertEquals(testString, ObjectRenderer.render(objectVO));

    }

    private void testArray(Object[] array, int expand) {
        ObjectVO objectVO = inspectObject(array, expand);
        Assert.assertEquals(array.getClass().getSimpleName(), objectVO.getType());
        Assertions.assertThat((Object[])objectVO.getValue())
                .hasSize(Math.min(array.length, arrayLenLimit))
                .contains(inspectObject(array[0], expand-1))
                .contains(inspectObject(array[1], expand-1))
                .contains(inspectObject(array[2], expand-1));

        printObject(objectVO);

    }

    private void testSimpleList(Collection collection) {
        ObjectVO objectVO = inspectObject(collection, 1);
        Assert.assertEquals(collection.getClass().getSimpleName(), objectVO.getType());

        printObject(objectVO);
    }

    private void testList(Collection collection, int expand) {
        Object[] array = collection.toArray();
        ObjectVO objectVO = inspectObject(collection, expand);
        Assert.assertEquals(collection.getClass().getSimpleName(), objectVO.getType());

        Assertions.assertThat(objectVO.getValue())
                .asList()
                .hasSize(collection.size())
                .contains(inspectObject(array[0], expand-1))
                .contains(inspectObject(array[1], expand-1))
                .contains(inspectObject(array[2], expand-1));

        printObject(objectVO);
    }

    private void testSimpleObject(Object value, String test) {
        ObjectVO objectVO = inspectObject(value, 1);
        printObject(objectVO);

        Assert.assertEquals(value.getClass().getSimpleName(), objectVO.getType());
        Assert.assertEquals(value, objectVO.getValue());
        Assert.assertEquals(test, ObjectRenderer.render(objectVO));
    }

    private String replaceHashCode(String input) {
        return input.replaceAll("@[0-9a-f]+", "@ffffffff");
    }

    private static class NestedClass {

        private int code;

        private static NestedClass c1 = get(1);
        private static NestedClass c2 = get(2);

        public NestedClass(int code) {
            this.code = code;
        }

        private static NestedClass get(int code) {
            return new NestedClass(code);
        }
    }
}
