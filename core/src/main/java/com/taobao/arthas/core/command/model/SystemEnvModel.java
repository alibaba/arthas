package com.taobao.arthas.core.command.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * sysenv KV Result
 * @author gongdewei 2020/4/2
 */
public class SystemEnvModel extends ResultModel {

    private Map<String, String> env = new TreeMap<String, String>();

    public SystemEnvModel() {
    }

    public SystemEnvModel(Map env) {
        this.putAll(env);
    }

    public SystemEnvModel(String name, String value) {
        this.put(name, value);
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String put(String key, String value) {
        return env.put(key, value);
    }

    public void putAll(Map m) {
        env.putAll(m);
    }

    @Override
    public String getType() {
        return "sysenv";
    }
}
