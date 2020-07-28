package com.taobao.arthas.core.command.model;

/**
 * Class detail VO
 * @author gongdewei 2020/4/8
 */
public class ClassDetailVO extends ClassVO {

    private String classInfo;
    private String codeSource;
    private boolean isInterface;
    private boolean isAnnotation;
    private boolean isEnum;
    private boolean isAnonymousClass;
    private boolean isArray;
    private boolean isLocalClass;
    private boolean isMemberClass;
    private boolean isPrimitive;
    private boolean isSynthetic;
    private String simpleName;
    private String modifier;
    private String[] annotations;
    private String[] interfaces;
    private String[] superClass;
    private FieldVO[] fields;

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

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    public boolean isAnnotation() {
        return isAnnotation;
    }

    public void setAnnotation(boolean annotation) {
        isAnnotation = annotation;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }

    public boolean isAnonymousClass() {
        return isAnonymousClass;
    }

    public void setAnonymousClass(boolean anonymousClass) {
        isAnonymousClass = anonymousClass;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public boolean isLocalClass() {
        return isLocalClass;
    }

    public void setLocalClass(boolean localClass) {
        isLocalClass = localClass;
    }

    public boolean isMemberClass() {
        return isMemberClass;
    }

    public void setMemberClass(boolean memberClass) {
        isMemberClass = memberClass;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public void setPrimitive(boolean primitive) {
        isPrimitive = primitive;
    }

    public boolean isSynthetic() {
        return isSynthetic;
    }

    public void setSynthetic(boolean synthetic) {
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

    public FieldVO[] getFields() {
        return fields;
    }

    public void setFields(FieldVO[] fields) {
        this.fields = fields;
    }

}
