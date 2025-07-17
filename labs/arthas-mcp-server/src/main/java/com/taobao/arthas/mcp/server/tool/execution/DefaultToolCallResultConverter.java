package com.taobao.arthas.mcp.server.tool.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A default implementation of {@link ToolCallResultConverter}.
 */
public final class DefaultToolCallResultConverter implements ToolCallResultConverter {

	private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallResultConverter.class);
	private static final ObjectMapper OBJECT_MAPPER = JsonParser.getObjectMapper();

	@Override
	public String convert(Object result, Type returnType) {
		if (returnType == Void.TYPE) {
			logger.debug("The tool has no return type. Converting to conventional response.");
			return JsonParser.toJson("Done");
		}
		if (result instanceof RenderedImage) {
			final ByteArrayOutputStream buf = new ByteArrayOutputStream(1024 * 4);
			try {
				ImageIO.write((RenderedImage) result, "PNG", buf);
			}
			catch (IOException e) {
				return "Failed to convert tool result to a base64 image: " + e.getMessage();
			}
			final String imgB64 = Base64.getEncoder().encodeToString(buf.toByteArray());

			Map<String, String> imageData = new HashMap<>();
			imageData.put("mimeType", "image/png");
			imageData.put("data", imgB64);

			return JsonParser.toJson(imageData);
		}
		else if (result instanceof String) {
			String stringResult = (String) result;

			if (isValidJson(stringResult)) {
				logger.debug("Result is already valid JSON, processing nested JSON strings.");
				return processNestedJsonStrings(stringResult);
			} else {
				logger.debug("Converting string result to JSON.");
				return JsonParser.toJson(result);
			}
		}
		else {
			logger.debug("Converting tool result to JSON.");
			return JsonParser.toJson(result);
		}
	}

	/**
	 * 检查字符串是否为有效的JSON格式
	 */
	private boolean isValidJson(String jsonString) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			return false;
		}
		
		try {
			OBJECT_MAPPER.readTree(jsonString);
			return true;
		} catch (JsonProcessingException e) {
			return false;
		}
	}

	/**
	 * 处理嵌套的JSON字符串，将字符串形式的JSON字段解析为实际的JSON对象
	 */
	private String processNestedJsonStrings(String jsonString) {
		try {
			JsonNode rootNode = OBJECT_MAPPER.readTree(jsonString);

			if (rootNode.isObject()) {
				ObjectNode objectNode = (ObjectNode) rootNode;

				String[] fieldsToProcess = {"results", "data", "content", "output"};
				
				for (String fieldName : fieldsToProcess) {
					JsonNode fieldNode = objectNode.get(fieldName);
					if (fieldNode != null && fieldNode.isTextual()) {
						String fieldValue = fieldNode.asText();
						if (isValidJson(fieldValue)) {
							try {
								JsonNode parsedFieldNode = OBJECT_MAPPER.readTree(fieldValue);
								objectNode.set(fieldName, parsedFieldNode);
								logger.debug("Successfully parsed nested JSON in field: {}", fieldName);
							} catch (JsonProcessingException e) {
								logger.debug("Failed to parse nested JSON in field {}: {}", fieldName, e.getMessage());
							}
						}
					}
				}
				
				return OBJECT_MAPPER.writeValueAsString(objectNode);
			}

			return jsonString;
			
		} catch (JsonProcessingException e) {
			logger.warn("Failed to process nested JSON strings: {}", e.getMessage());
			return jsonString;
		}
	}

}
