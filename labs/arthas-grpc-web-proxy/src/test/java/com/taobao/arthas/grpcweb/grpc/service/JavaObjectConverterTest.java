package com.taobao.arthas.grpcweb.grpc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.taobao.arthas.grpcweb.grpc.objectUtils.JavaObjectConverter;
import org.junit.Assert;
import org.junit.Test;

import io.arthas.api.ArthasServices.ArrayElement;
import io.arthas.api.ArthasServices.ArrayValue;
import io.arthas.api.ArthasServices.BasicValue;
import io.arthas.api.ArthasServices.CollectionValue;
import io.arthas.api.ArthasServices.JavaField;
import io.arthas.api.ArthasServices.JavaFields;
import io.arthas.api.ArthasServices.JavaObject;
import io.arthas.api.ArthasServices.MapEntry;
import io.arthas.api.ArthasServices.MapValue;
import io.arthas.api.ArthasServices.NullValue;

public class JavaObjectConverterTest {

    @Test
    public void testString() {
        JavaObject javaObject = JavaObjectConverter.toJavaObject("sss");
        System.err.println(javaObject);
        assertNotNull(javaObject);
        assertEquals("java.lang.String", javaObject.getClassName());
        assertTrue(javaObject.hasBasicValue());
        assertEquals("sss", javaObject.getBasicValue().getString());
    }

    @Test
    public void testToJavaObjectWithBasicType() {
        int intValue = 123;
        JavaObject javaObject = JavaObjectConverter.toJavaObject(intValue);
        assertNotNull(javaObject);
        assertEquals("java.lang.Integer", javaObject.getClassName());
        assertTrue(javaObject.hasBasicValue());
        assertEquals(intValue, javaObject.getBasicValue().getInt());
    }

    @Test
    public void testToJavaObjectWithArray() {
        int[] intArray = { 1, 2, 3 };
        JavaObject javaObject = JavaObjectConverter.toJavaObject(intArray);
        assertNotNull(javaObject);
        assertEquals("[I", javaObject.getClassName());
        assertTrue(javaObject.hasArrayValue());
        ArrayValue arrayValue = javaObject.getArrayValue();
        assertNotNull(arrayValue);
        assertEquals("int", arrayValue.getClassName());
        assertEquals(3, arrayValue.getElementsCount());

        ArrayElement element = arrayValue.getElements(1);
        assertEquals(2, element.getBasicValue().getInt());
    }

    @Test
    public void testToJavaObjectWithMultiDimensionalArray() {
        int[][] multiDimensionalArray = { { 1, 2, 3 }, { 4, 5, 6 } };

        JavaObject javaObject = JavaObjectConverter.toJavaObjectWithExpand(multiDimensionalArray,2);
        assertNotNull(javaObject);
        assertEquals("[[I", javaObject.getClassName());
        assertTrue(javaObject.hasArrayValue());
        ArrayValue arrayValue = javaObject.getArrayValue();
        assertNotNull(arrayValue);
        assertEquals("[I", arrayValue.getClassName());
        assertEquals(2, arrayValue.getElementsCount());

        ArrayElement element = arrayValue.getElements(0);
        assertTrue(element.hasArrayValue());

        ArrayValue arrayValue1 = element.getArrayValue();
        assertEquals("int", arrayValue1.getClassName());
        ArrayElement element1 = arrayValue1.getElements(0);
        assertEquals(3, arrayValue1.getElementsCount());

        assertTrue(element1.hasBasicValue());
        BasicValue basicValue = element1.getBasicValue();
        assertEquals(1, basicValue.getInt());
    }

    @Test
    public void testToJavaObjectWithCollection() {
        List<String> stringList = new ArrayList<>();
        stringList.add("foo");
        stringList.add("bar");
        stringList.add("baz");
        JavaObject javaObject = JavaObjectConverter.toJavaObject(stringList);
        assertNotNull(javaObject);
        assertEquals("java.util.ArrayList", javaObject.getClassName());
        assertTrue(javaObject.hasCollection());
        CollectionValue collectionValue = javaObject.getCollection();
        assertNotNull(collectionValue);
        assertEquals(3, collectionValue.getElementsCount());

        JavaObject object3 = collectionValue.getElements(2);
        assertEquals("baz", object3.getBasicValue().getString());
    }

    @Test
    public void testToJavaObjectWithMap() {
        Map<String, Integer> stringIntegerMap = new HashMap<>();
        stringIntegerMap.put("one", 1);
        stringIntegerMap.put("two", 2);
        JavaObject javaObject = JavaObjectConverter.toJavaObject(stringIntegerMap);
        assertNotNull(javaObject);
        assertEquals("java.util.HashMap", javaObject.getClassName());
        assertTrue(javaObject.hasMap());
        MapValue mapValue = javaObject.getMap();
        assertNotNull(mapValue);
        assertEquals(2, mapValue.getEntriesCount());

        MapEntry mapEntry = mapValue.getEntries(0);

        JavaObject key = mapEntry.getKey();
        assertEquals("one", key.getBasicValue().getString());
        JavaObject value = mapEntry.getValue();
        assertEquals(1, value.getBasicValue().getInt());
    }

    @Test
    public void testToJavaObject() {
        // 创建一个复杂的 Object
        ComplexObject complexObject = createComplexObject();

        // 转换为 JavaObject
        JavaObject javaObject = JavaObjectConverter.toJavaObject(complexObject);

        // 对转换后的 JavaObject 进行断言，验证各个 field 的值是否一致
        Assert.assertNotNull(javaObject);
        Assert.assertEquals(ComplexObject.class.getName(), javaObject.getClassName());

        JavaFields fields = javaObject.getFields();

        Map<String, JavaField> fieldMap = fields.getFieldsList().stream()
                .collect(Collectors.toMap(JavaField::getName, field -> field));

        // 验证基础类型字段
        BasicValue basicValue = fieldMap.get("basicValue").getBasicValue();
        Assert.assertEquals(5, basicValue.getInt());

        // 验证集合字段
        JavaField collection = fieldMap.get("collection");
        CollectionValue collectionValue = collection.getCollection();

        Assert.assertEquals(2, collectionValue.getElementsCount());

        // 验证数组字段
        JavaField array = fieldMap.get("arrayValue");
        ArrayValue arrayValue = array.getArrayValue();
        Assert.assertEquals(2, arrayValue.getElementsCount());

        // 验证嵌套对象字段
        JavaField nestedObject = fieldMap.get("nestedObject");
        JavaObject nestedJavaObject = nestedObject.getObjectValue();
        JavaFields nestedObjectFields = nestedJavaObject.getFields();
        Assert.assertEquals(1, nestedObjectFields.getFieldsCount());
        JavaField nestedObjectField = nestedObjectFields.getFields(0);
        Assert.assertEquals("stringValue", nestedObjectField.getName());
        Assert.assertEquals("nestedValue", nestedObjectField.getBasicValue().getString());
    }

    private ComplexObject createComplexObject() {
        ComplexObject complexObject = new ComplexObject();
        complexObject.setBasicValue(5);
        complexObject.setCollection(Arrays.asList("element1", "element2"));
        complexObject.setArrayValue(new int[] { 1, 2 });
        complexObject.setNestedObject(new NestedObject("nestedValue"));
        return complexObject;
    }

    private static class ComplexObject {
        private int basicValue;
        private Collection<String> collection;
        private int[] arrayValue;
        private NestedObject nestedObject;

        public int getBasicValue() {
            return basicValue;
        }

        public void setBasicValue(int basicValue) {
            this.basicValue = basicValue;
        }

        public Collection<String> getCollection() {
            return collection;
        }

        public void setCollection(Collection<String> collection) {
            this.collection = collection;
        }

        public int[] getArrayValue() {
            return arrayValue;
        }

        public void setArrayValue(int[] arrayValue) {
            this.arrayValue = arrayValue;
        }

        public NestedObject getNestedObject() {
            return nestedObject;
        }

        public void setNestedObject(NestedObject nestedObject) {
            this.nestedObject = nestedObject;
        }
    }

    private static class NestedObject {
        private String stringValue;

        public NestedObject(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }
    }

    private static class TestObject {
        private Double[] doubleArray;

        public Double[] getDoubleArray() {
            return doubleArray;
        }

        public void setDoubleArray(Double[] doubleArray) {
            this.doubleArray = doubleArray;
        }
    }

    @Test
    public void testObjectWithDubboArrayField() {
        // 创建测试对象
        TestObject testObject = new TestObject();
        testObject.setDoubleArray(new Double[] { 1.0, 2.0, 3.0 });

        // 转换为JavaObject
        JavaObject javaObject = JavaObjectConverter.toJavaObject(testObject);

        // 检查各个field的值是否一致
        for (int i = 0; i < testObject.getDoubleArray().length; i++) {
            Double expectedValue = testObject.getDoubleArray()[i];
            ArrayValue arrayValue = javaObject.getFields().getFields(0).getArrayValue();
            ArrayElement arrayElement = arrayValue.getElements(i);
            Double actualValue = arrayElement.getBasicValue().getDouble();
            Assert.assertEquals(expectedValue, actualValue);
        }
    }

    @Test
    public void testToJavaObjectWithNullValue() {
        JavaObject result = JavaObjectConverter.toJavaObject(null);
        assertNotNull(result);
        assertTrue(result.hasNullValue());
        assertEquals(NullValue.getDefaultInstance(), result.getNullValue());
    }

    @Test
    public void testToJavaObjectWithNullKeyInMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "value");

        JavaObject result = JavaObjectConverter.toJavaObject(map);
        assertNotNull(result);
        assertTrue(result.hasMap());
        MapValue mapValue = result.getMap();
        assertEquals(1, mapValue.getEntriesCount());

        MapEntry entry = mapValue.getEntries(0);
        assertNotNull(entry.getKey());
        assertTrue(entry.getKey().hasNullValue());
        assertEquals(NullValue.getDefaultInstance(), entry.getKey().getNullValue());

        assertNotNull(entry.getValue());
        assertTrue(entry.getValue().hasBasicValue());

        assertEquals("value", entry.getValue().getBasicValue().getString());
    }

    @Test
    public void testToJavaObjectWithNullValueInArray() {
        Object[] array = new Object[3];
        array[0] = "value";
        array[1] = null;
        array[2] = 123;

        JavaObject result = JavaObjectConverter.toJavaObject(array);
        assertNotNull(result);
        assertTrue(result.hasArrayValue());
        ArrayValue arrayValue = result.getArrayValue();
        assertEquals(3, arrayValue.getElementsCount());

        ArrayElement element1 = arrayValue.getElements(0);
        assertTrue(element1.hasObjectValue());
        JavaObject objectValue1 = element1.getObjectValue();
        assertTrue(objectValue1.hasBasicValue());
        assertEquals("value", objectValue1.getBasicValue().getString());

        ArrayElement element2 = arrayValue.getElements(1);
        assertNotNull(element2.getNullValue());

        ArrayElement element3 = arrayValue.getElements(2);
        assertTrue(element3.hasObjectValue());
        JavaObject objectValue3 = element3.getObjectValue();
        assertTrue(objectValue3.hasBasicValue());
        assertEquals(123, objectValue3.getBasicValue().getInt());
    }
}