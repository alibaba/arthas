package com.taobao.arthas.mcp.server.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mcp specific objectVO serialization filter
 */
public class McpObjectVOFilter implements ValueFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(McpObjectVOFilter.class);
    
    @Override
    public Object apply(Object object, String name, Object value) {
        if (value == null) {
            return null;
        }
        String className = value.getClass().getSimpleName();
        if ("ObjectVO".equals(className)) {
            return handleObjectVO(value);
        }
        
        return value;
    }

    private Object handleObjectVO(Object objectVO) {
        try {
            Object innerObject = getFieldValue(objectVO, "object");
            Integer expand = (Integer) getFieldValue(objectVO, "expand");
            
            if (innerObject == null) {
                return "null";
            }

            return needExpand(expand) ? drawObjectView(innerObject) : objectToString(innerObject);
        } catch (Exception e) {
            logger.warn("Failed to handle ObjectVO: {}", e.getMessage());
            return "{\"error\":\"ObjectVO serialization failed\"}";
        }
    }

    private String drawObjectView(Object object) {
        try {
            JSONWriter.Context context = JSONFactory.createWriteContext();
            context.setMaxLevel(4097);
            context.config(JSONWriter.Feature.IgnoreErrorGetter, true);
            context.config(JSONWriter.Feature.ReferenceDetection, true);
            context.config(JSONWriter.Feature.IgnoreNonFieldGetter, true);
            context.config(JSONWriter.Feature.WriteNonStringKeyAsString, true);
            return JSON.toJSONString(object, context);
        } catch (Exception e) {
            logger.debug("ObjectView-style serialization failed, using toString: {}", e.getMessage());
            return objectToString(object);
        }
    }

    private String objectToString(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return object.toString();
        } catch (Exception e) {
            return object.getClass().getSimpleName() + "@" + Integer.toHexString(object.hashCode());
        }
    }

    private boolean needExpand(Integer expand) {
        return expand != null && expand > 0;
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            logger.debug("Failed to get field {} from {}: {}", fieldName, obj.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}
