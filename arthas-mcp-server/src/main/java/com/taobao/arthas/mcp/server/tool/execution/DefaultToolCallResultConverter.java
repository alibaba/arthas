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
 * {@link ToolCallResultConverter} 的默认实现，负责将 Tool 方法的返回值转换为字符串，
 * 以便通过 MCP 协议返回给 AI 模型。
 * <p>
 * 支持以下返回类型的转换：
 * <ul>
 *   <li><b>void/Void</b>：返回约定的完成提示字符串 {@code "Done"} 的 JSON 表示</li>
 *   <li><b>{@link RenderedImage}（图片）</b>：将图片编码为 PNG 格式的 Base64 字符串，
 *       并封装为包含 {@code mimeType} 和 {@code data} 字段的 JSON 对象</li>
 *   <li><b>{@link String}</b>：若字符串本身已是合法 JSON，则直接原样返回；
 *       否则将其序列化为 JSON 字符串（带引号的 JSON 字符串）返回</li>
 *   <li><b>其他对象</b>：通过 Jackson 序列化为 JSON 字符串返回</li>
 * </ul>
 * <p>
 * 本类为 {@code final}，不允许继承，如需自定义转换逻辑请实现 {@link ToolCallResultConverter} 接口。
 */
public final class DefaultToolCallResultConverter implements ToolCallResultConverter {

	private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallResultConverter.class);

	/**
	 * 共享的 Jackson ObjectMapper 实例，来自 {@link JsonParser#getObjectMapper()}，线程安全。
	 * 仅用于 {@link #isValidJson(String)} 方法中解析 JSON 字符串做合法性校验。
	 */
	private static final ObjectMapper OBJECT_MAPPER = JsonParser.getObjectMapper();

	/**
	 * 将 Tool 方法的返回值转换为可发送给 AI 模型的字符串。
	 * <p>
	 * 转换逻辑按以下顺序判断：
	 * <ol>
	 *   <li>若 {@code returnType} 为 {@code void}，直接返回 {@code "Done"} 的 JSON 表示，
	 *       表示工具已成功执行但无实质性返回内容。</li>
	 *   <li>若 {@code result} 是 {@link RenderedImage} 实例（图片），
	 *       则将图片写入内存缓冲区（PNG 格式），再经 Base64 编码，
	 *       最终返回包含 {@code mimeType}（固定为 {@code "image/png"}）
	 *       和 {@code data}（Base64 字符串）两个字段的 JSON 对象字符串。
	 *       若图片写入过程发生 IO 异常，则返回描述错误的字符串。</li>
	 *   <li>若 {@code result} 是 {@link String} 实例：
	 *       <ul>
	 *         <li>先通过 {@link #isValidJson(String)} 判断其是否已是合法 JSON，
	 *             若是则直接原样返回，避免二次序列化引入多余转义。</li>
	 *         <li>若不是合法 JSON，则调用 {@link JsonParser#toJson(Object)} 序列化后返回
	 *             （结果为带双引号的 JSON 字符串）。</li>
	 *       </ul>
	 *   </li>
	 *   <li>其他所有类型，调用 {@link JsonParser#toJson(Object)} 序列化为 JSON 字符串后返回。</li>
	 * </ol>
	 *
	 * @param result     Tool 方法的返回值；{@code returnType} 为 {@code void} 时此参数无意义
	 * @param returnType Tool 方法声明的泛型返回类型
	 * @return 转换后的字符串，将作为 MCP Tool 调用结果发送给 AI 模型
	 */
	@Override
	public String convert(Object result, Type returnType) {
		if (returnType == Void.TYPE) {
			// 无返回值的方法：返回约定的 "Done" 作为成功提示
			logger.debug("The tool has no return type. Converting to conventional response.");
			return JsonParser.toJson("Done");
		}
		if (result instanceof RenderedImage) {
			// 图片类型：将 RenderedImage 编码为 PNG 格式，再转为 Base64 字符串
			final ByteArrayOutputStream buf = new ByteArrayOutputStream(1024 * 4);
			try {
				// 使用 Java ImageIO 将图片以 PNG 格式写入内存缓冲区
				ImageIO.write((RenderedImage) result, "PNG", buf);
			}
			catch (IOException e) {
				// 图片编码失败时返回错误描述，而不是抛出异常，避免中断整个请求处理链
				return "Failed to convert tool result to a base64 image: " + e.getMessage();
			}
			// 将内存中的 PNG 字节数组编码为 Base64 字符串
			final String imgB64 = Base64.getEncoder().encodeToString(buf.toByteArray());

			// 构造包含 mimeType 和 data 字段的 Map，序列化为 JSON 返回
			Map<String, String> imageData = new HashMap<>();
			imageData.put("mimeType", "image/png");
			imageData.put("data", imgB64);

			return JsonParser.toJson(imageData);
		}
		else if (result instanceof String) {
			String stringResult = (String) result;
			if (isValidJson(stringResult)) {
				// 已是合法 JSON 字符串，直接返回，避免二次序列化导致字符串被额外加引号或转义
				logger.debug("Result is already valid JSON, returning as is.");
				return stringResult;
			} else {
				// 普通字符串，包装为 JSON 字符串（加双引号）后返回
				logger.debug("Converting string result to JSON.");
				return JsonParser.toJson(result);
			}
		}
		else {
			// 其他 Java 对象，统一序列化为 JSON 字符串
			logger.debug("Converting tool result to JSON.");
			return JsonParser.toJson(result);
		}
	}

	/**
	 * 判断给定字符串是否为合法的 JSON。
	 * <p>
	 * 通过尝试用 Jackson 的 {@link ObjectMapper#readTree(String)} 解析字符串来验证合法性：
	 * 若解析成功则为合法 JSON，若抛出 {@link JsonProcessingException} 则不是合法 JSON。
	 * <p>
	 * 以下情况直接返回 {@code false}，不做 JSON 解析：
	 * <ul>
	 *   <li>{@code jsonString} 为 {@code null}</li>
	 *   <li>{@code jsonString} 去除首尾空白后为空字符串</li>
	 * </ul>
	 *
	 * @param jsonString 待检查的字符串
	 * @return 若为合法 JSON 则返回 {@code true}，否则返回 {@code false}
	 */
	private boolean isValidJson(String jsonString) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			// 空字符串不是合法 JSON，快速返回，避免不必要的解析开销
			return false;
		}
		try {
			// 尝试解析，无异常则为合法 JSON
			OBJECT_MAPPER.readTree(jsonString);
			return true;
		} catch (JsonProcessingException e) {
			// 解析失败，说明不是合法 JSON
			return false;
		}
	}

}
