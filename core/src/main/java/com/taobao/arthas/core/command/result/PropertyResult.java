package com.taobao.arthas.core.command.result;

import java.util.HashMap;
import java.util.Map;

/**
 * Property KV Result
 * @author gongdewei 2020/4/2
 */
public class PropertyResult extends ExecResult {

    private Map<String, String> props = new HashMap<String, String>();

    public PropertyResult() {
    }

    public PropertyResult(Map props) {
        this.putAll(props);
    }

    public PropertyResult(String name, String value) {
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
        return "property";
    }
}
