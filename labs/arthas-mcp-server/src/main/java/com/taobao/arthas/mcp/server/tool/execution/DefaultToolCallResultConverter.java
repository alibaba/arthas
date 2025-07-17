package com.taobao.arthas.mcp.server.tool.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
				logger.debug("Result is already valid JSON, returning as is.");
				return stringResult;
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

}
