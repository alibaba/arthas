package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/20
 */
public class WelcomeModel extends ResultModel {

    private String pid;
    private String time;
    private String version;
    private String wiki;
    private String tutorials;
    private String mainClass;

    public WelcomeModel() {
    }

    @Override
    public String getType() {
        return "welcome";
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWiki() {
        return wiki;
    }

    public void setWiki(String wiki) {
        this.wiki = wiki;
    }

    public String getTutorials() {
        return tutorials;
    }

    public void setTutorials(String tutorials) {
        this.tutorials = tutorials;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
}
