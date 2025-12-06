package com.taobao.arthas.mcp.server.tool.validator;

import com.taobao.arthas.mcp.server.protocol.spec.JsonSchemaValidator;
import com.taobao.arthas.mcp.server.protocol.spec.JsonSchemaValidatorSupplier;

public class FastJsonJsonSchemaValidatorSupplier implements JsonSchemaValidatorSupplier {
    @Override
    public JsonSchemaValidator get() {
        return new FastJsonJsonSchemaValidator();
    }
}
