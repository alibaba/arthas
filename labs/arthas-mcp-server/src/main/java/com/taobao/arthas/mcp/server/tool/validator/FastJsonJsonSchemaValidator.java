package com.taobao.arthas.mcp.server.tool.validator;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.schema.ValidateResult;
import com.taobao.arthas.mcp.server.protocol.spec.JsonSchemaValidator;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FastJsonJsonSchemaValidator implements JsonSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(FastJsonJsonSchemaValidator.class);

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Object structuredContent) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema must not be null");
        }
        if (structuredContent == null) {
            throw new IllegalArgumentException("Structured content must not be null");
        }

        try {
            // Create schema validator from map
            JSONObject schemaJson = JSONObject.from(schema);
            JSONSchema jsonSchema = JSONSchema.of(schemaJson);
            
            Object contentToValidate = structuredContent;
            if (structuredContent instanceof String) {
                try {
                    contentToValidate = JSON.parse((String) structuredContent);
                } catch (Exception e) {
                     return ValidationResponse.asInvalid("Invalid JSON string content: " + e.getMessage());
                }
            } else {
                // Ensure it is compatible with fastjson2 validation
                // JSONObject.from handles POJOs and Maps
                contentToValidate = JSONObject.from(structuredContent);
            }

            ValidateResult result = jsonSchema.validate(contentToValidate);

            if (result.isSuccess()) {
                 String jsonString = (structuredContent instanceof String) ? (String) structuredContent : JsonParser.toJson(structuredContent);
                 return ValidationResponse.asValid(jsonString);
            } else {
                return ValidationResponse.asInvalid("Validation failed: " + result.getMessage());
            }

        } catch (Exception e) {
            logger.error("Failed to validate content against schema", e);
            return ValidationResponse.asInvalid("Unexpected validation error: " + e.getMessage());
        }
    }
}
