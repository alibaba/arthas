package com.taobao.arthas.core.command.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Property KV Result
 * @author gongdewei 2020/4/2
 */
public class SystemPropertyModel extends ResultModel {

    private Map<String, String> props = new HashMap<String, String>();

    public SystemPropertyModel() {
    }

    public SystemPropertyModel(Map props) {
        this.putAll(props);
    }

    public SystemPropertyModel(String name, String value) {
        this.put(name, value);
    }

    public Map<String, String> getProps() {
        return props;
    }

    public String put(String key, String value) {
        return props.put(key, value);
    }

    public void putAll(Map m) {
        props.putAll(m);
    }

    @Override
    public String getType() {
        return "sysprop";
    }
}
