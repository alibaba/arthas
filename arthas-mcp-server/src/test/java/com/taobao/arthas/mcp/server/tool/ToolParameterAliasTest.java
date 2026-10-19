package com.taobao.arthas.mcp.server.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.annotation.ToolParam;
import com.taobao.arthas.mcp.server.tool.definition.ToolDefinitions;
import com.taobao.arthas.mcp.server.tool.execution.DefaultToolCallResultConverter;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the input schema advertised through the public {@link ToolCallback} library API and
 * the arguments accepted by {@link ToolCallback#call(String)} agree when a tool parameter is renamed
 * with {@link JsonProperty}.
 */
class ToolParameterAliasTest {

    private static final String ALIAS = "message_text";

    private static final String JAVA_NAME = "message";

    @Test
    void requiredAliasShouldBeAdvertisedAndBoundFromAliasJson() {
        ToolCallback callback = callback("requiredAlias", String.class);
        McpSchema.JsonSchema schema = callback.getToolDefinition().getInputSchema();

        assertThat(schema.getProperties()).containsKey(ALIAS).doesNotContainKey(JAVA_NAME);
        assertThat(schema.getRequired()).containsExactly(ALIAS);
        assertThat(schema.getAdditionalProperties()).isFalse();

        assertThat(callback.call(aliasInput("hello"))).isEqualTo(JsonParser.toJson("echo:hello"));
    }

    @Test
    void optionalAliasShouldBindProvidedValue() {
        ToolCallback callback = callback("optionalAlias", String.class);

        assertThat(callback.getToolDefinition().getInputSchema().getProperties()).containsKey(ALIAS);
        assertThat(callback.getToolDefinition().getInputSchema().getRequired()).isEmpty();

        assertThat(callback.call(aliasInput("hello"))).isEqualTo(JsonParser.toJson("hello"));
    }

    @Test
    void optionalAliasOmittedShouldKeepNullValue() {
        ToolCallback callback = callback("optionalAlias", String.class);

        assertThat(callback.call("{}")).isEqualTo("null");
    }

    @Test
    void parameterWithoutAliasShouldKeepJavaName() {
        ToolCallback callback = callback("plainName", String.class);

        assertThat(callback.getToolDefinition().getInputSchema().getProperties())
                .containsKey(JAVA_NAME)
                .doesNotContainKey(ALIAS);

        assertThat(callback.call("{\"message\":\"hello\"}")).isEqualTo(JsonParser.toJson("hello"));
    }

    @Test
    void emptyAliasShouldFallBackToJavaName() {
        ToolCallback callback = callback("emptyAlias", String.class);

        assertThat(callback.getToolDefinition().getInputSchema().getProperties())
                .containsKey(JAVA_NAME)
                .doesNotContainKey(ALIAS);

        assertThat(callback.call("{\"message\":\"hello\"}")).isEqualTo(JsonParser.toJson("hello"));
    }

    @Test
    void missingRequiredAliasShouldReportAdvertisedName() {
        ToolCallback callback = callback("requiredAlias", String.class);

        assertThatThrownBy(() -> callback.call("{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Required parameter 'message_text' is missing");

        assertThatThrownBy(() -> callback.call("{\"message\":\"hello\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Required parameter 'message_text' is missing");
    }

    @Test
    void aliasShouldBindWhileToolContextIsInjected() {
        ToolCallback callback = callback("aliasWithContext", String.class, ToolContext.class);

        assertThat(callback.getToolDefinition().getInputSchema().getProperties()).containsOnlyKeys(ALIAS);

        Map<String, Object> contextValues = new HashMap<String, Object>();
        contextValues.put(ToolContextKeys.COMMAND_CONTEXT, "ctx-1");

        assertThat(callback.call(aliasInput("hello"), new ToolContext(contextValues)))
                .isEqualTo(JsonParser.toJson("hello|ctx-1"));
    }

    private static String aliasInput(String value) {
        return "{\"" + ALIAS + "\":\"" + value + "\"}";
    }

    private static ToolCallback callback(String methodName, Class<?>... parameterTypes) {
        try {
            Method method = AliasedToolFixture.class.getMethod(methodName, parameterTypes);
            return DefaultToolCallback.builder()
                    .toolDefinition(ToolDefinitions.from(method))
                    .toolMethod(method)
                    .toolObject(new AliasedToolFixture())
                    .toolCallResultConverter(new DefaultToolCallResultConverter())
                    .build();
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Fixture method not found: " + methodName, ex);
        }
    }

    public static final class AliasedToolFixture {

        @Tool(name = "required_alias", description = "Echo the required aliased text argument.")
        public String requiredAlias(
                @ToolParam(description = "message text")
                @JsonProperty("message_text") String message) {
            return "echo:" + message;
        }

        @Tool(name = "optional_alias", description = "Return the optional aliased text argument.")
        public String optionalAlias(
                @ToolParam(description = "message text", required = false)
                @JsonProperty("message_text") String message) {
            return message;
        }

        @Tool(name = "plain_name", description = "Control tool without a parameter alias.")
        public String plainName(
                @ToolParam(description = "message text") String message) {
            return message;
        }

        @Tool(name = "empty_alias", description = "Control tool with an empty parameter alias.")
        public String emptyAlias(
                @ToolParam(description = "message text")
                @JsonProperty("") String message) {
            return message;
        }

        @Tool(name = "alias_with_context", description = "Echo the aliased text argument with the tool context.")
        public String aliasWithContext(
                @ToolParam(description = "message text")
                @JsonProperty("message_text") String message,
                ToolContext toolContext) {
            return message + "|" + toolContext.getContext().get(ToolContextKeys.COMMAND_CONTEXT);
        }
    }
}
