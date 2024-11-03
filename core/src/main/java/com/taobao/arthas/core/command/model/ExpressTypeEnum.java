package com.taobao.arthas.core.command.model;

/**
 * @Author TaoKan
 * @Date 2024/9/22 7:32 AM
 */
public enum ExpressTypeEnum
{
    OGNL("ognl"),
    QLEXPRESS("qlexpress");

    private String expressType;

    ExpressTypeEnum(String expressType) {
        this.expressType = expressType;
    }

    public String getExpressType() {
        return expressType;
    }
}
