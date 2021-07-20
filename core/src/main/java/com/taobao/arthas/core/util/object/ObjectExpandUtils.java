package com.taobao.arthas.core.util.object;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.StringUtils;

/**
 * Object expand
 * @author gongdewei 2020/9/28
 */
public abstract class ObjectExpandUtils {

    public static Object expand(Object value, Integer expand) {
        return expand(value, expand, null);
    }

    public static Object expand(Object value, Integer expand, Integer sizeLimit) {
        if (GlobalOptions.isUsingJson) {
            return value;
        } else {
            if (expand != null && expand >= 0) {
                ObjectInspector objectInspector;
                if (sizeLimit != null) {
                    objectInspector = new ObjectInspector(sizeLimit);
                } else {
                    objectInspector = new ObjectInspector();
                }
                return objectInspector.inspect(value, expand);
            } else {
                return value;
            }
        }
    }

    /**
     * render expand or raw object
     * @param obj expand or raw object
     * @return
     */
    public static String toString(Object obj) {
        if (obj instanceof ObjectVO){
            return ObjectRenderer.render((ObjectVO) obj);
        } else {
            if (GlobalOptions.isUsingJson) {
                return JSON.toJSONString(obj, SerializerFeature.IgnoreErrorGetter);
            } else {
                return StringUtils.objectToString(obj);
            }
        }
    }

}
