package com.taobao.arthas.grpcweb.grpc.objectUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
// ComplexObject.java
public class ComplexObject {
    private int id;
    private String name;
    private double value;
    private int[] numbers;
    private Long[] longNumbers;
    private NestedObject nestedObject;
    private ComplexObject[] complexArray;
    private int[][] multiDimensionalArray;
    private String[] stringArray;

    private Collection<String> collection;

    List<String> stringList;

    Map<String, Integer> stringIntegerMap;

    private Double[] doubleArray;

    public static class NestedObject {
        private int nestedId;
        private String nestedName;
        private boolean flag;

        public int getNestedId() {
            return nestedId;
        }

        public void setNestedId(int nestedId) {
            this.nestedId = nestedId;
        }

        public String getNestedName() {
            return nestedName;
        }

        public void setNestedName(String nestedName) {
            this.nestedName = nestedName;
        }

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

    }


    public Map<String, Integer> getStringIntegerMap() {
        return stringIntegerMap;
    }

    public void setStringIntegerMap(Map<String, Integer> stringIntegerMap) {
        this.stringIntegerMap = stringIntegerMap;
    }

    public Double[] getDoubleArray() {
        return doubleArray;
    }

    public void setDoubleArray(Double[] doubleArray) {
        this.doubleArray = doubleArray;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int[] getNumbers() {
        return numbers;
    }

    public void setNumbers(int[] numbers) {
        this.numbers = numbers;
    }

    public NestedObject getNestedObject() {
        return nestedObject;
    }

    public void setNestedObject(NestedObject nestedObject) {
        this.nestedObject = nestedObject;
    }

    public ComplexObject[] getComplexArray() {
        return complexArray;
    }

    public void setComplexArray(ComplexObject[] complexArray) {
        this.complexArray = complexArray;
    }

    public int[][] getMultiDimensionalArray() {
        return multiDimensionalArray;
    }

    public void setMultiDimensionalArray(int[][] multiDimensionalArray) {
        this.multiDimensionalArray = multiDimensionalArray;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public Long[] getLongNumbers() {
        return longNumbers;
    }

    public void setLongNumbers(Long[] longNumbers) {
        this.longNumbers = longNumbers;
    }

    public Collection<String> getCollection() {
        return collection;
    }

    public void setCollection(Collection<String> collection) {
        this.collection = collection;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }
}
