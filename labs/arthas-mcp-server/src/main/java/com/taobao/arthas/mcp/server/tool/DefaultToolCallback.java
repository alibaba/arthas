package com.taobao.arthas.mcp.server.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;
import com.taobao.arthas.mcp.server.tool.execution.ToolCallResultConverter;
import com.taobao.arthas.mcp.server.tool.execution.ToolExecutionException;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.JsonParser;
import com.taobao.arthas.mcp.server.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class DefaultToolCallback implements ToolCallback{

    private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallback.class);

    private final ToolDefinition toolDefinition;

    private final Method toolMethod;

    private final Object toolObject;

    private final ToolCallResultConverter toolCallResultConverter;


    public DefaultToolCallback(ToolDefinition toolDefinition, Method toolMethod,
                              Object toolObject, ToolCallResultConverter toolCallResultConverter) {
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        Assert.notNull(toolMethod, "toolMethod cannot be null");
        Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
                "toolObject cannot be null for non-static methods");
        this.toolDefinition = toolDefinition;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallResultConverter = toolCallResultConverter;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");

        logger.debug("Starting execution of tool: {}", this.toolDefinition.getName());

        validateToolContextSupport(toolContext);

        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

        Object result = callMethod(methodArguments);

        logger.debug("Successful execution of tool: {}", this.toolDefinition.getName());

        Type returnType = this.toolMethod.getGenericReturnType();

        return this.toolCallResultConverter.convert(result, returnType);
    }

    private void validateToolContextSupport(ToolContext toolContext) {
        boolean isNonEmptyToolContextProvided = toolContext != null && !Utils.isCollectionEmpty(toolContext.getContext());

        boolean isToolContextAcceptedByMethod = Arrays.stream(this.toolMethod.getParameterTypes())
                .anyMatch(type -> Utils.isAssignable(type, ToolContext.class));

        if (isToolContextAcceptedByMethod && !isNonEmptyToolContextProvided) {
            throw new IllegalArgumentException("ToolContext is required by the method as an argument");
        }
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
        return JsonParser.fromJson(toolInput, new TypeReference<Map<String, Object>>() {
        });
    }

    private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, ToolContext toolContext) {
        return Stream.of(this.toolMethod.getParameters()).map(parameter -> {
            if (parameter.getType().isAssignableFrom(ToolContext.class)) {
                return toolContext;
            }
            Object rawArgument = toolInputArguments.get(parameter.getName());
            return buildTypedArgument(rawArgument, parameter.getParameterizedType());
        }).toArray();
    }

    private Object buildTypedArgument(Object value, Type type) {
        if (value == null) {
            return null;
        }

        if (type instanceof Class<?>) {
            return JsonParser.toTypedObject(value, (Class<?>) type);
        }

        String json = JsonParser.toJson(value);
        return JsonParser.fromJson(json, type);
    }

    private Object callMethod(Object[] methodArguments) {
        if (isObjectNotPublic() || isMethodNotPublic()) {
            this.toolMethod.setAccessible(true);
        }
        Object result;
        try {
            result = this.toolMethod.invoke(this.toolObject, methodArguments);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        }
        catch (InvocationTargetException ex) {
            throw new ToolExecutionException(this.toolDefinition, ex.getCause());
        }
        return result;
    }

    private boolean isObjectNotPublic() {
        return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
    }

    private boolean isMethodNotPublic() {
        return !Modifier.isPublic(this.toolMethod.getModifiers());
    }

    @Override
    public String toString() {
        return "MethodToolCallback{" + "toolDefinition=" + this.toolDefinition + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ToolDefinition toolDefinition;

        private Method toolMethod;

        private Object toolObject;

        private ToolCallResultConverter toolCallResultConverter;

        private Builder() {
        }

        public Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }


        public Builder toolMethod(Method toolMethod) {
            this.toolMethod = toolMethod;
            return this;
        }

        public Builder toolObject(Object toolObject) {
            this.toolObject = toolObject;
            return this;
        }

        public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
            this.toolCallResultConverter = toolCallResultConverter;
            return this;
        }


        public DefaultToolCallback build() {
            return new DefaultToolCallback(this.toolDefinition, this.toolMethod, this.toolObject, this.toolCallResultConverter);
        }

    }
}
