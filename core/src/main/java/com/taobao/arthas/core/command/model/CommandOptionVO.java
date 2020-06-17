package com.taobao.arthas.core.command.model;


/**
 * @author gongdewei 2020/4/3
 */
public class CommandOptionVO {
    /**
     * the option long name.
     */
    private String longName;

    /**
     * the option short name.
     */
    private String shortName;

    /**
     * The option description.
     */
    private String description;

    /**
     * whether or not the option receives a single value or  multiple values.
     */
    private boolean acceptValue;

    public CommandOptionVO() {
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAcceptValue() {
        return acceptValue;
    }

    public void setAcceptValue(boolean acceptValue) {
        this.acceptValue = acceptValue;
    }
}
