package com.taobao.arthas.bytekit.asm;

/**
 *
 * @author hengyunabc 2019-03-18
 *
 */
public class MethodInfo {

    private String owner;

    private int access;
    private String name;
    private String desc;

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

}
