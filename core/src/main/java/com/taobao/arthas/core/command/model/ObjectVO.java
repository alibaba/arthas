package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.ObjectVOUtils;

import java.util.Collection;
import java.util.List;

/**
 * @author gongdewei 2020/4/29
 */
public class ObjectVO {

    //ClassName/TypeName
    protected String type;

    //FieldName
    protected String name;

    // value maybe: primitives/box, string, collection/array, ObjectVO
    protected Object value;

    //map entry key
    protected ObjectVO key;

    // string, collection/array
    private Integer size;

    //complex object's fields
    protected List<ObjectVO> fields;

    public ObjectVO(String type) {
        this.type = type;
    }

    public ObjectVO(String type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static ObjectVO ofFields(String type, List<ObjectVO> fields) {
        ObjectVO objectVO = new ObjectVO(type);
        objectVO.setFields(fields);
        return objectVO;
    }

    public static ObjectVO ofArray(String type, int size, Object[] value) {
        ObjectVO objectVO = new ObjectVO(type);
        objectVO.setSize(size);
        objectVO.setValue(value);
        return objectVO;
    }

    public static ObjectVO ofCollection(String type, int size, Collection value) {
        ObjectVO objectVO = new ObjectVO(type);
        objectVO.setSize(size);
        objectVO.setValue(value);
        return objectVO;
    }

    public static ObjectVO ofString(String type, int size, String value) {
        ObjectVO objectVO = new ObjectVO(type);
        objectVO.setSize(value.length());
        objectVO.setValue(value);
        return objectVO;
    }

    public static ObjectVO ofKeyValue(ObjectVO key, ObjectVO value) {
        ObjectVO objectVO = new ObjectVO(null);
        objectVO.setKey(key);
        objectVO.setValue(value);
        return objectVO;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<ObjectVO> getFields() {
        return fields;
    }

    public void setFields(List<ObjectVO> fields) {
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectVO getKey() {
        return key;
    }

    public void setKey(ObjectVO key) {
        this.key = key;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectVO objectVO = (ObjectVO) o;

        if (type != null ? !type.equals(objectVO.type) : objectVO.type != null) return false;
        if (name != null ? !name.equals(objectVO.name) : objectVO.name != null) return false;
        if (value != null ? !value.equals(objectVO.value) : objectVO.value != null) return false;
        if (key != null ? !key.equals(objectVO.key) : objectVO.key != null) return false;
        if (size != null ? !size.equals(objectVO.size) : objectVO.size != null) return false;
        return fields != null ? fields.equals(objectVO.fields) : objectVO.fields == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ObjectVOUtils.toString(this,"");
    }


}
