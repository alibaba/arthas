package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * Data model of GetStaticCommand
 * @author gongdewei 2020/4/20
 */
public class GetStaticModel extends ResultModel {

    private Collection<ClassVO> matchedClasses;
    private String fieldName;
    private ObjectVO field;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

    public GetStaticModel() {
    }

    public GetStaticModel(String fieldName, Object fieldValue, int expand) {
        this.fieldName = fieldName;
        this.field = new ObjectVO(fieldValue, expand);
    }

    public GetStaticModel(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    public ObjectVO getField() {
        return field;
    }

    public void setField(ObjectVO field) {
        this.field = field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    public void setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public GetStaticModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public GetStaticModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    @Override
    public String getType() {
        return "getstatic";
    }
}
