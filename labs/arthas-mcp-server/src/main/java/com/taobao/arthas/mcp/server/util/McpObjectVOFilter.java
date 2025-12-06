package com.taobao.arthas.mcp.server.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.core.command.model.ObjectVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mcp specific ObjectVO serialization filter
 */
public class McpObjectVOFilter implements ValueFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(McpObjectVOFilter.class);
    
    @Override
    public Object apply(Object object, String name, Object value) {
        if (value == null) {
            return null;
        }
        
        // Direct type check instead of reflection
        if (value instanceof ObjectVO) {
            return handleObjectVO((ObjectVO) value);
        }
        
        return value;
    }

    private Object handleObjectVO(ObjectVO objectVO) {
        try {
            Object innerObject = objectVO.getObject();
            Integer expand = objectVO.getExpand();
            
            if (innerObject == null) {
                return "null";
            }

            return objectVO.needExpand() ? drawObjectView(innerObject) : objectToString(innerObject);
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
}
