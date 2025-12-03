package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.Map;

/**
 * Interface for validating structured content against a JSON schema.
 */
public interface JsonSchemaValidator {

    /**
     * Represents the result of a validation operation.
     */
    class ValidationResponse {
        private final boolean valid;
        private final String errorMessage;
        private final String jsonStructuredOutput;

        public ValidationResponse(boolean valid, String errorMessage, String jsonStructuredOutput) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.jsonStructuredOutput = jsonStructuredOutput;
        }

        public static ValidationResponse asValid(String jsonStructuredOutput) {
            return new ValidationResponse(true, null, jsonStructuredOutput);
        }

        public static ValidationResponse asInvalid(String message) {
            return new ValidationResponse(false, message, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getJsonStructuredOutput() {
            return jsonStructuredOutput;
        }
    }

    /**
     * Validates the structured content against the provided JSON schema.
     * @param schema The JSON schema to validate against.
     * @param structuredContent The structured content to validate.
     * @return A ValidationResponse indicating whether the validation was successful.
     */
    ValidationResponse validate(Map<String, Object> schema, Object structuredContent);
}
