package com.taobao.arthas.core.mcp.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mcp specific ObjectVO serialization filter
 */
public class McpObjectVOFilter implements ValueFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(McpObjectVOFilter.class);
    
    private static final McpObjectVOFilter INSTANCE = new McpObjectVOFilter();
    private static volatile boolean registered = false;
    
    /**
     * Register this filter to JsonParser
     */
    public static void register() {
        if (!registered) {
            synchronized (McpObjectVOFilter.class) {
                if (!registered) {
                    JsonParser.registerFilter(INSTANCE);
                    registered = true;
                    logger.debug("McpObjectVOFilter registered to JsonParser");
                }
            }
        }
    }
    
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

            if (objectVO.needExpand()) {
                // 根据 GlobalOptions.isUsingJson 配置决定输出格式
                if (GlobalOptions.isUsingJson) {
                    return drawJsonView(innerObject);
                } else {
                    return drawObjectView(objectVO);
                }
            } else {
                return objectToString(innerObject);
            }
        } catch (Exception e) {
            logger.warn("Failed to handle ObjectVO: {}", e.getMessage());
            return "{\"error\":\"ObjectVO serialization failed\"}";
        }
    }

    /**
     * 使用 ObjectView 输出对象结构
     */
    private String drawObjectView(ObjectVO objectVO) {
        try {
            ObjectView objectView = new ObjectView(objectVO);
            return objectView.draw();
        } catch (Exception e) {
            logger.debug("ObjectView serialization failed, using toString: {}", e.getMessage());
            return objectToString(objectVO.getObject());
        }
    }

    /**
     * 使用 JSON 格式输出对象
     */
    private String drawJsonView(Object object) {
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
