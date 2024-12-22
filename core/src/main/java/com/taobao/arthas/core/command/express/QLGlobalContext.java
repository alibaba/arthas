package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
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
    private static final Logger logger = LoggerFactory.getLogger(QLGlobalContext.class);

    private Map<String, Object> context;
    private Object object;
    private ReflectLoader reflectLoader;

    public QLGlobalContext(ReflectLoader reflectLoader) {
        this.context = new ConcurrentHashMap<>();
        this.reflectLoader = reflectLoader;
    }

    public void put(String name, Object value){
        context.put(name, value);
    }

    public void clear() {
        context.clear();
        this.context.put("reflectLoader",reflectLoader);
    }

    public void bindObj(Object object) {
        this.object = object;
        context.put("object",object);
    }
    @Override
    public Value get(Map<String, Object> attachments, String variableName) {
        if ((this.reflectLoader != null) && (this.object != null) && !variableName.startsWith("#")) {
            return this.reflectLoader.loadField(this.object, variableName, true, PureErrReporter.INSTANCE);
        }
        String newVariableName = variableName.replace("#","");
        return new MapItemValue(this.context, newVariableName);
    }


    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

}
