package com.taobao.arthas.grpcweb.grpc.objectUtils;

import arthas.grpc.api.ArthasService.JavaField;
import arthas.grpc.api.ArthasService.JavaObject;
import arthas.grpc.api.ArthasService.NullValue;
import arthas.grpc.api.ArthasService.ArrayValue;
import arthas.grpc.api.ArthasService.UnexpandedObject;
import arthas.grpc.api.ArthasService.BasicValue;
import arthas.grpc.api.ArthasService.ArrayElement;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class JavaObjectConverter {
    private static final int MAX_DEPTH = 3;

    public static JavaObject toJavaObject(Object obj) {
        return toJavaObject(obj, 0);
    }

    public static JavaObject toJavaObject(Object obj, int depth) {
        if (obj == null || depth >= MAX_DEPTH) {
            return null;
        }

        JavaObject.Builder objectBuilder = JavaObject.newBuilder();
        objectBuilder.setClassName(obj.getClass().getName());

        if(obj.getClass().isPrimitive() || isBasicType(obj.getClass())){
            BasicValue basicValue = createBasicValue(obj);
            JavaField javaField = JavaField.newBuilder().setBasicValue(basicValue).build();
            return objectBuilder.addFields(javaField).build();
        }

        Field[] fields = obj.getClass().getDeclaredFields();
        List<JavaField> javaFields = new ArrayList<>();

        for (Field field : fields) {
            field.setAccessible(true);
            JavaField.Builder fieldBuilder = JavaField.newBuilder();
            fieldBuilder.setName(field.getName());

            try {
                Object fieldValue = field.get(obj);
                Class<?> fieldType = field.getType();

                if (fieldValue == null) {
                    fieldBuilder.setNullValue(NullValue.newBuilder().setClassName(fieldType.getName()).build());
                } else if (fieldType.isArray()) {
                    ArrayValue arrayValue = toArrayValue(fieldValue, depth + 1);
                    if (arrayValue != null) {
                        fieldBuilder.setArrayValue(arrayValue);
                    } else {
                        fieldBuilder.setUnexpandedObject(
                                UnexpandedObject.newBuilder().setClassName(fieldType.getName()).build());
                    }
                } else if (fieldType.isPrimitive() || isBasicType(fieldType)) {
                    BasicValue basicValue = createBasicValue(fieldValue);
                    fieldBuilder.setBasicValue(basicValue);
                } else {
                    JavaObject nestedObject = toJavaObject(fieldValue, depth + 1);
                    if (nestedObject != null) {
                        fieldBuilder.setObjectValue(nestedObject);
                    } else {
                        fieldBuilder.setUnexpandedObject(
                                UnexpandedObject.newBuilder().setClassName(fieldType.getName()).build());
                    }
                }
            } catch (IllegalAccessException e) {
                // Handle the exception appropriately
                e.printStackTrace();
            }

            javaFields.add(fieldBuilder.build());
        }

        objectBuilder.addAllFields(javaFields);
        return objectBuilder.build();
    }

    private static ArrayValue toArrayValue(Object array, int depth) {
        if (array == null || depth >= MAX_DEPTH) {
            return null;
        }

        ArrayValue.Builder arrayBuilder = ArrayValue.newBuilder();
        Class<?> componentType = array.getClass().getComponentType();

        arrayBuilder.setClassName(componentType.getName());

        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);

            if (element != null) {
                if (componentType.isArray()) {
                    ArrayValue nestedArrayValue = toArrayValue(element, depth + 1);
                    if (nestedArrayValue == null) {
                        arrayBuilder.addElements(ArrayElement.newBuilder().setNullValue(NullValue.newBuilder().setClassName(componentType.getName())));
                    } else {
                        arrayBuilder.addElements(ArrayElement.newBuilder().setUnexpandedObject(
                                UnexpandedObject.newBuilder().setClassName(element.getClass().getName()).build()));
                    }

                } else if (componentType.isPrimitive() || isBasicType(componentType)) {
                    BasicValue basicValue = createBasicValue(element);
                    arrayBuilder.addElements(ArrayElement.newBuilder().setBasicValue(basicValue));
                } else {
                    JavaObject nestedObject = toJavaObject(element, depth + 1);
                    if (nestedObject != null) {
                        arrayBuilder.addElements(ArrayElement.newBuilder().setObjectValue(nestedObject));
                    } else {
                        arrayBuilder.addElements(ArrayElement.newBuilder().setUnexpandedObject(
                                UnexpandedObject.newBuilder().setClassName(element.getClass().getName()).build()));
                    }

                }
            } else {
                arrayBuilder.addElements(ArrayElement.newBuilder()
                        .setNullValue(NullValue.newBuilder().setClassName(componentType.getName()).build()));
            }
        }

        return arrayBuilder.build();
    }

    private static BasicValue createBasicValue(Object value) {
        BasicValue.Builder builder = BasicValue.newBuilder();

        if (value instanceof Integer) {
            builder.setIntValue((int) value);
        } else if (value instanceof Long) {
            builder.setLongValue((long) value);
        } else if (value instanceof Float) {
            builder.setFloatValue((float) value);
        } else if (value instanceof Double) {
            builder.setDoubleValue((double) value);
        } else if (value instanceof Boolean) {
            builder.setBooleanValue((boolean) value);
        } else if (value instanceof String) {
            builder.setStringValue((String) value);
        }

        return builder.build();
    }

    private static  boolean isBasicType(Class<?> classType){
        return classType.equals(Integer.class) ||
                classType.equals(Long.class) ||
                classType.equals(Float.class) ||
                classType.equals(Double.class) ||
                classType.equals(Boolean.class) ||
                classType.equals(String.class);
    }
}