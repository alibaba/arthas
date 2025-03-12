package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.exception.PureErrReporter;
import com.alibaba.qlexpress4.runtime.ReflectLoader;
import com.alibaba.qlexpress4.runtime.Value;
import com.alibaba.qlexpress4.runtime.context.ExpressContext;
import com.alibaba.qlexpress4.runtime.context.ObjectFieldExpressContext;
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
    private ObjectFieldExpressContext objectFieldExpressContext;
    private Express4Runner express4Runner;

    public QLGlobalContext(Express4Runner expressRunner) {
        this.context = new ConcurrentHashMap<>();
        this.express4Runner = expressRunner;
    }

    public void put(String name, Object value){
        context.put(name, value);
    }

    public void clear() {
        context.clear();
    }

    public void bindObj(Object object) {
        ObjectFieldExpressContext objectFieldExpressContext = new ObjectFieldExpressContext(object, express4Runner);
        this.objectFieldExpressContext = objectFieldExpressContext;
        context.put("object",object);
    }
    @Override
    public Value get(Map<String, Object> attachments, String variableName) {
        Value getFromLoadField = objectFieldExpressContext.get(attachments, variableName);
        if (getFromLoadField == null || getFromLoadField.get() == null) {
            return new MapItemValue(this.context, variableName);
        }
        return getFromLoadField;
    }


    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

}
