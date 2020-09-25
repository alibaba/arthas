package com.taobao.arthas.core.model;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.ObjectUtils;
import com.taobao.arthas.core.util.ObjectVOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gongdewei 2020/9/24
 */
public class ObjectVOTest {

    @Test
    public void testPrimitiveTypes() {

        testSimpleObject(Integer.valueOf(100));
        testSimpleObject(Long.valueOf(1000L));
        testSimpleObject(Short.valueOf((short) 100));
        testSimpleObject((byte) 100);
        testSimpleObject((char) 100);
        testSimpleObject(true);
        testSimpleObject(100.0f);
        testSimpleObject(100.0);

        testSimpleObject("100.0");
    }

    @Test
    public void testComplexObjects() {
        testComplexObject(new AtomicInteger(1));
        testComplexObject(new Random());
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
        System.out.println("expand 3: ");
        testList(atomicIntegers, 3);
        System.out.println("expand 4: ");
        testList(atomicIntegers, 4);
    }

    @Test
    public void testArrays() {
        testPrimitiveArray(new int[]{1, 2, 3}, null);
        testPrimitiveArray(new short[]{1, 2, 3}, null);
        testPrimitiveArray(new byte[]{1, 2, 3}, null);
        testPrimitiveArray(new long[]{1, 2, 3}, null);
        testPrimitiveArray(new float[]{1, 2, 3}, null);
        testPrimitiveArray(new double[]{1, 2, 3}, null);

        testPrimitiveArray(new Integer[]{1, 2, 3}, null);
        testPrimitiveArray(new Short[]{1, 2, 3}, null);
        testPrimitiveArray(new Byte[]{1, 2, 3}, null);
        testPrimitiveArray(new Long[]{1L, 2L, 3L}, null);
        testPrimitiveArray(new Float[]{1f, 2f, 3f}, null);
        testPrimitiveArray(new Double[]{1.0, 2.0, 3.0}, null);

//        testPrimitiveArray(new char[]{1, 2, 3}, new Object[]{"SOH", "STX", "ETX"});
//        testPrimitiveArray(new Character[]{1, 2, 3}, new Object[]{"SOH", "STX", "ETX"});
        testPrimitiveArray(new char[]{1, 2, 3, '\n', '\r'}, new Object[]{"\\u0001", "\\u0002", "\\u0003", "\\u000A", "\\u000D"});
        testPrimitiveArray(new Character[]{1, 2, 3, '\n', '\r'}, new Object[]{"\\u0001", "\\u0002", "\\u0003", "\\u000A", "\\u000D"});

        testArray(new Object[]{1.0, 2L, 3.0f}, 2);

        testArray(new String[]{"str1", "2", "3"}, 2);

        //Object[]
        testArray(new Object[]{new AtomicInteger(1), new AtomicInteger(2), new AtomicInteger(3)}, 2);
        testArray(new AtomicInteger[]{new AtomicInteger(1), new AtomicInteger(2), new AtomicInteger(3)}, 2);

    }

    @Test
    public void testMaps() {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("strValue", "Arthas中国");
        map.put("intValue", 1234);
        map.put("boolValue", true);
        map.put("doubleValue", 123.0);
        map.put("floatValue", 123.0f);
        map.put("listValue", Arrays.asList(1,2,3));
        map.put("dateValue", new Date());
        map.put("objectValue", new Random());

        ObjectVO objectVO1 = ObjectVOUtils.inspectObject(map, 1);
        System.out.println(JSON.toJSONString(objectVO1));
        System.out.println();
        System.out.println(objectVO1);
        System.out.println();


        ObjectVO objectVO2 = ObjectVOUtils.inspectObject(map, 2);

        System.out.println(JSON.toJSONString(objectVO2));
        System.out.println();
        System.out.println(objectVO2);
        System.out.println();
    }

    private void testPrimitiveArray(Object array, Object[] testArray) {
        if (testArray == null) {
            testArray = ObjectUtils.toObjectArray(array);
        }
        ObjectVO objectVO = ObjectVOUtils.inspectObject(array, 2);
        Assert.assertEquals(array.getClass().getSimpleName(), objectVO.getType());
        Assertions.assertThat(objectVO.getValue())
                .isEqualTo(testArray);

        System.out.println(JSON.toJSONString(objectVO));
        System.out.println();
        System.out.println(objectVO);
        System.out.println();
    }

    private void testArray(Object[] array, int expand) {
        ObjectVO objectVO = ObjectVOUtils.inspectObject(array, expand);
        Assert.assertEquals(array.getClass().getSimpleName(), objectVO.getType());
        Assertions.assertThat((Object[])objectVO.getValue())
                .hasSize(array.length)
                .contains(ObjectVOUtils.inspectObject(array[0], expand - 1))
                .contains(ObjectVOUtils.inspectObject(array[1], expand - 1))
                .contains(ObjectVOUtils.inspectObject(array[2], expand - 1));

        System.out.println(JSON.toJSONString(objectVO));
        System.out.println();
        System.out.println(objectVO);
        System.out.println();

    }

    private void testSimpleList(Collection origin) {
        ObjectVO objectVO = ObjectVOUtils.inspectObject(origin, 2);
        Assert.assertEquals(origin.getClass().getSimpleName(), objectVO.getType());

        System.out.println(JSON.toJSONString(objectVO));
        System.out.println();
        System.out.println(objectVO);
        System.out.println();
    }

    private void testList(Collection origin, int expand) {
        List list = new ArrayList(origin);
        ObjectVO objectVO = ObjectVOUtils.inspectObject(origin, expand);
        Assert.assertEquals(origin.getClass().getSimpleName(), objectVO.getType());

        Assertions.assertThat(objectVO.getValue())
                .asList()
                .hasSize(origin.size())
                .contains(ObjectVOUtils.inspectObject(list.get(0), expand - 1))
                .contains(ObjectVOUtils.inspectObject(list.get(1), expand - 1))
                .contains(ObjectVOUtils.inspectObject(list.get(2), expand - 1));

        System.out.println(JSON.toJSONString(objectVO));
        System.out.println();
        System.out.println(objectVO);
        System.out.println();
    }

    private void testSimpleObject(Object value) {
        ObjectVO objectVO = ObjectVOUtils.inspectObject(value, 2);
        Assert.assertEquals(value.getClass().getSimpleName(), objectVO.getType());
        Assert.assertEquals(value, objectVO.getValue());
        System.out.println(JSON.toJSONString(objectVO));
        System.out.println();
        System.out.println(objectVO);
        System.out.println();
    }

    private void testComplexObject(Object value) {
        ObjectVO objectVO = ObjectVOUtils.inspectObject(value, 2);
        Assert.assertEquals(value.getClass().getSimpleName(), objectVO.getType());
        //Assert.assertEquals(value, objectVO.getValue());
        //fields
        Field[] fields = value.getClass().getDeclaredFields();
        for (Field field : fields) {
            // TODO
            //  Assertions.assertThat(objectVO.getFields()).con
        }

        System.out.println(JSON.toJSONString(objectVO));
        System.out.println();
        System.out.println(objectVO);
        System.out.println();
    }
}
