package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/8
 */
public class ClassInfoVO {

    private String name;
    private String classInfo;
    private String codeSource;
    private Boolean isInterface;
    private Boolean isAnnotation;
    private Boolean isEnum;
    private Boolean isAnonymousClass;
    private Boolean isArray;
    private Boolean isLocalClass;
    private Boolean isMemberClass;
    private Boolean isPrimitive;
    private Boolean isSynthetic;
    private String simpleName;
    private String modifier;
    private String[] annotations;
    private String[] interfaces;
    private String[] superClass;
    private String[] classloader;
    private String classLoaderHash;
    private FieldVO[] fields;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(String classInfo) {
        this.classInfo = classInfo;
    }

    public String getCodeSource() {
        return codeSource;
    }

    public void setCodeSource(String codeSource) {
        this.codeSource = codeSource;
    }

    public Boolean getInterface() {
        return isInterface;
    }

    public void setInterface(Boolean anInterface) {
        isInterface = anInterface;
    }

    public Boolean getAnnotation() {
        return isAnnotation;
    }

    public void setAnnotation(Boolean annotation) {
        isAnnotation = annotation;
    }

    public Boolean getEnum() {
        return isEnum;
    }

    public void setEnum(Boolean anEnum) {
        isEnum = anEnum;
    }

    public Boolean getAnonymousClass() {
        return isAnonymousClass;
    }

    public void setAnonymousClass(Boolean anonymousClass) {
        isAnonymousClass = anonymousClass;
    }

    public Boolean getArray() {
        return isArray;
    }

    public void setArray(Boolean array) {
        isArray = array;
    }

    public Boolean getLocalClass() {
        return isLocalClass;
    }

    public void setLocalClass(Boolean localClass) {
        isLocalClass = localClass;
    }

    public Boolean getMemberClass() {
        return isMemberClass;
    }

    public void setMemberClass(Boolean memberClass) {
        isMemberClass = memberClass;
    }

    public Boolean getPrimitive() {
        return isPrimitive;
    }

    public void setPrimitive(Boolean primitive) {
        isPrimitive = primitive;
    }

    public Boolean getSynthetic() {
        return isSynthetic;
    }

    public void setSynthetic(Boolean synthetic) {
        isSynthetic = synthetic;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public String[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    public String[] getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String[] superClass) {
        this.superClass = superClass;
    }

    public String[] getClassloader() {
        return classloader;
    }

    public void setClassloader(String[] classloader) {
        this.classloader = classloader;
    }

    public String getClassLoaderHash() {
        return classLoaderHash;
    }

    public void setClassLoaderHash(String classLoaderHash) {
        this.classLoaderHash = classLoaderHash;
    }

    public FieldVO[] getFields() {
        return fields;
    }

    public void setFields(FieldVO[] fields) {
        this.fields = fields;
    }
}
