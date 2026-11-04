package com.taobao.arthas.core.command.model;

/**
 * Method or Constructor VO
 * @author gongdewei 2020/4/9
 */
public class MethodVO {

    private String declaringClass;
    private String methodName;
    private String modifier;
    private String[] annotations;
    private String[] parameters;
    private String returnType;
    private String[] exceptions;
    private String classLoaderHash;
    private String descriptor;
    private boolean constructor;

    public String getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(String declaringClass) {
        this.declaringClass = declaringClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String[] getExceptions() {
        return exceptions;
    }

    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }

    public String getClassLoaderHash() {
        return classLoaderHash;
    }

    public void setClassLoaderHash(String classLoaderHash) {
        this.classLoaderHash = classLoaderHash;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
}
