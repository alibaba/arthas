package com.taobao.arthas.core.command.express;

import com.alibaba.qlexpress4.exception.PureErrReporter;
import com.alibaba.qlexpress4.runtime.ReflectLoader;
import com.alibaba.qlexpress4.runtime.Value;
import com.alibaba.qlexpress4.runtime.context.ExpressContext;
import com.alibaba.qlexpress4.runtime.data.MapItemValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author TaoKan
 * @Date 2024/9/22 12:39 PM
 */
public class QLGlobalContext implements ExpressContext {
    private Map<String, Object> context;
    private Object object;
    private ReflectLoader reflectLoader;

    public QLGlobalContext(Map<String, Object> context, Object bindObject, ReflectLoader reflectLoader) {
        this.context = context;
        this.object = bindObject;
        this.reflectLoader = reflectLoader;
    }

    public QLGlobalContext() {
        this.context = new ConcurrentHashMap<>();
    }

    public void put(String name, Object value){
        context.put(name, value);
    }

    public void clear() {
        context.clear();
    }

    public void bindObj(Object object) {
        this.object = object;
    }
    @Override
    public Value get(Map<String, Object> attachments, String variableName) {
        if ((this.reflectLoader != null) && (this.object != null) && !variableName.startsWith("#")) {
            return this.reflectLoader.loadField(this.object, variableName, true, PureErrReporter.INSTANCE);
        }
        return new MapItemValue(this.context, variableName);
    }
}
