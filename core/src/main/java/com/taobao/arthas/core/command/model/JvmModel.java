package com.taobao.arthas.core.command.model;

import java.util.*;

/**
 * Model of 'jvm' command
 *
 * @author gongdewei 2020/4/24
 */
public class JvmModel extends ResultModel {

    private Map<String, List<JvmItemVO>> jvmInfo;

    public JvmModel() {
        jvmInfo = Collections.synchronizedMap(new LinkedHashMap<String, List<JvmItemVO>>());
    }

    @Override
    public String getType() {
        return "jvm";
    }

    public JvmModel addItem(String group, String name, Object value) {
        this.addItem(group, name, value, null);
        return this;
    }

    public JvmModel  addItem(String group, String name, Object value, String desc) {
        this.group(group).add(new JvmItemVO(name, value, desc));
        return this;
    }

    public List<JvmItemVO> group(String group) {
        synchronized (this) {
            List<JvmItemVO> list = jvmInfo.get(group);
            if (list == null) {
                list = new ArrayList<JvmItemVO>();
                jvmInfo.put(group, list);
            }
            return list;
        }
    }

    public Map<String, List<JvmItemVO>> getJvmInfo() {
        return jvmInfo;
    }

}
