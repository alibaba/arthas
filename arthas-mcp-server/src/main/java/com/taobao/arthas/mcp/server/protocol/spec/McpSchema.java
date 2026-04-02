/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * 基于JSON-RPC 2.0规范和模型上下文协议(MCP)规范的核心模式定义类
 *
 * <p>此类提供了MCP协议的完整数据模型定义，包括：
 * <ul>
 *   <li>JSON-RPC消息类型（请求、响应、通知）</li>
 *   <li>初始化和握手协议</li>
 *   <li>资源管理和访问</li>
 *   <li>提示词(Prompt)管理</li>
 *   <li>工具(Tool)调用</li>
 *   <li>采样和推理功能</li>
 *   <li>日志和进度通知</li>
 *   <li>分页支持</li>
 * </ul>
 *
 * <p>相关规范：
 * <a href="http://www.jsonrpc.org/specification">JSON-RPC 2.0 specification</a>
 * <a href="https://github.com/modelcontextprotocol/specification/blob/main/schema/2024-11-05/schema.ts">
 *     Model Context Protocol Schema</a>
 *
 * @author Yeaury
 */
public final class McpSchema {

	/** 日志记录器，用于记录协议相关的事件和错误 */
	private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

	/** 私有构造函数，防止实例化工具类 */
	private McpSchema() {
	}

    /** 最新协议版本常量 */
    public static final String LATEST_PROTOCOL_VERSION = ProtocolVersions.MCP_2025_06_18;

	/** JSON-RPC协议版本字符串 */
	public static final String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// 方法名称常量定义
	// ---------------------------

	// 生命周期方法
	/** 初始化方法名：客户端向服务器发送初始化请求 */
	public static final String METHOD_INITIALIZE = "initialize";

	/** 已完成初始化通知：客户端通知服务器初始化完成 */
	public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	/** Ping方法：用于保持连接活跃 */
	public static final String METHOD_PING = "ping";

	/** 进度通知：报告长时间操作的进度 */
	public static final String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";

	// 工具方法
	/** 列出可用工具的方法名 */
	public static final String METHOD_TOOLS_LIST = "tools/list";

	/** 调用工具的方法名 */
	public static final String METHOD_TOOLS_CALL = "tools/call";

	/** 工具列表变化通知：通知客户端可用工具列表已更改 */
	public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	// 资源方法
	/** 列出可用资源的方法名 */
	public static final String METHOD_RESOURCES_LIST = "resources/list";

	/** 读取资源内容的方法名 */
	public static final String METHOD_RESOURCES_READ = "resources/read";

	/** 资源列表变化通知：通知客户端可用资源列表已更改 */
	public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	/** 列出资源模板的方法名 */
	public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	// 提示词方法
	/** 列出可用提示词的方法名 */
	public static final String METHOD_PROMPT_LIST = "prompts/list";

	/** 获取特定提示词的方法名 */
	public static final String METHOD_PROMPT_GET = "prompts/get";

	/** 提示词列表变化通知：通知客户端可用提示词列表已更改 */
	public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	// 日志方法
	/** 设置日志级别的方法名 */
	public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	/** 消息通知：向客户端发送日志消息 */
	public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// 根目录方法
	/** 列出根目录的方法名 */
	public static final String METHOD_ROOTS_LIST = "roots/list";

	/** 根目录列表变化通知 */
	public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// 采样方法
	/** 创建采样消息的方法：请求LLM生成响应 */
	public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	// 引导方法
    /** 创建引导请求的方法 */
    public static final String METHOD_ELICITATION_CREATE = "elicitation/create";

	/** Jackson对象映射器，用于JSON序列化和反序列化 */
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// ---------------------------
	// JSON-RPC错误码定义
	// ---------------------------
	/**
	 * MCP JSON-RPC响应中使用的标准错误码
	 *
	 * <p>这些错误码遵循JSON-RPC 2.0规范，用于标识不同类型的错误情况
	 */
	public static final class ErrorCodes {

		/**
		 * 解析错误：服务器接收到的JSON无效
		 * <p>当JSON格式不正确或无法解析时返回此错误
		 */
		public static final int PARSE_ERROR = -32700;

		/**
		 * 无效请求：发送的JSON不是有效的请求对象
		 * <p>当JSON结构不符合JSON-RPC请求格式时返回此错误
		 */
		public static final int INVALID_REQUEST = -32600;

		/**
		 * 方法未找到：请求的方法不存在或不可用
		 * <p>当请求的方法名在服务器中不存在时返回此错误
		 */
		public static final int METHOD_NOT_FOUND = -32601;

		/**
		 * 无效参数：方法参数无效
		 * <p>当方法参数类型、数量或格式不正确时返回此错误
		 */
		public static final int INVALID_PARAMS = -32602;

		/**
		 * 内部错误：JSON-RPC内部错误
		 * <p>当服务器在处理请求时发生未预期的内部错误时返回此错误
		 */
		public static final int INTERNAL_ERROR = -32603;

	}

    /**
     * 元数据接口，提供额外的元信息
     *
     * <p>实现此接口的类可以提供额外的元数据信息，
     * 用于扩展协议功能和传递额外上下文
     */
    public interface Meta {

        /**
         * 获取元数据映射
         *
         * @return 包含元数据的键值对映射，如果无元数据则返回null
         */
        default Map<String, Object> meta() {
            return null;
        }

    }

	/**
	 * 请求接口，扩展了元数据接口
	 *
	 * <p>所有MCP请求类型都应实现此接口，提供对进度令牌的访问
	 */
	public interface Request extends Meta {

        /**
         * 从元数据中提取进度令牌
         *
         * <p>进度令牌用于跟踪长时间运行的操作进度
         *
         * @return 进度令牌对象，如果未设置则返回null
         */
        default Object progressToken() {
            // 获取元数据映射
            Map<String, Object> metadata = meta();
            // 检查元数据中是否包含进度令牌
            if (metadata != null && metadata.containsKey("progressToken")) {
                return metadata.get("progressToken");
            }
            return null;
        }
	}

	/**
	 * 结果接口，扩展了元数据接口
	 *
	 * <p>所有MCP响应结果类型都应实现此接口
	 */
	public interface Result extends Meta {
	}

	/** 类型引用，用于将JSON反序列化为HashMap */
	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	/**
	 * 将JSON字符串反序列化为JSONRPCMessage对象
	 *
	 * <p>此方法根据JSON结构自动判断消息类型并返回相应的实例：
	 * <ul>
	 *   <li>包含method和id字段 → JSONRPCRequest（请求）</li>
	 *   <li>包含method但不包含id字段 → JSONRPCNotification（通知）</li>
	 *   <li>包含result或error字段 → JSONRPCResponse（响应）</li>
	 * </ul>
	 *
	 * @param objectMapper 用于反序列化的ObjectMapper实例
	 * @param jsonText 要反序列化的JSON字符串
	 * @return JSONRPCMessage实例，使用{@link JSONRPCRequest}、
	 *         {@link JSONRPCNotification}或{@link JSONRPCResponse}类
	 * @throws IOException 如果反序列化过程中发生错误
	 * @throws IllegalArgumentException 如果JSON结构不匹配任何已知消息类型
	 */
	public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
			throws IOException {

		// 记录接收到的JSON消息，便于调试
		logger.debug("Received JSON message: {}", jsonText);

		// 将JSON字符串反序列化为Map以便检查结构
		Map<String, Object> map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

		// 根据JSON结构判断消息类型
		// 包含method和id字段的是请求消息
		if (map.containsKey("method") && map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCRequest.class);
		}
		// 包含method但不包含id字段的是通知消息
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCNotification.class);
		}
		// 包含result或error字段的是响应消息
		else if (map.containsKey("result") || map.containsKey("error")) {
			return objectMapper.convertValue(map, JSONRPCResponse.class);
		}

		// 无法识别的消息类型，抛出异常
		throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
	}

	// ---------------------------
	// JSON-RPC消息类型定义
	// ---------------------------

	/**
	 * JSON-RPC消息接口
	 *
	 * <p>所有JSON-RPC消息类型都必须实现此接口
	 */
	public interface JSONRPCMessage {
		/**
		 * 获取JSON-RPC协议版本
		 * @return 协议版本字符串，通常为"2.0"
		 */
		String getJsonrpc();
	}

	/**
	 * JSON-RPC请求消息
	 *
	 * <p>表示从客户端发送到服务器的请求，包含方法名、参数和请求ID
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCRequest implements JSONRPCMessage {
		/** JSON-RPC协议版本 */
		private final String jsonrpc;
		/** 要调用的方法名 */
		private final String method;
		/** 请求标识符，用于关联响应 */
		private final Object id;
		/** 方法参数，可以是位置参数或命名参数 */
		private final Object params;

		/**
		 * 创建JSON-RPC请求消息
		 *
		 * @param jsonrpc JSON-RPC协议版本
		 * @param method 要调用的方法名
		 * @param id 请求标识符
		 * @param params 方法参数
		 */
		public JSONRPCRequest(
				@JsonProperty("jsonrpc") String jsonrpc,
				@JsonProperty("method") String method,
				@JsonProperty("id") Object id,
				@JsonProperty("params") Object params) {
			this.jsonrpc = jsonrpc;
			this.method = method;
			this.id = id;
			this.params = params;
		}

		@Override
		public String getJsonrpc() {
			return jsonrpc;
		}

		public String getMethod() {
			return method;
		}

		public Object getId() {
			return id;
		}

		public Object getParams() {
			return params;
		}
	}

	/**
	 * JSON-RPC通知消息
	 *
	 * <p>通知是不需要响应的消息，不包含请求ID字段
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCNotification implements JSONRPCMessage {
		/** JSON-RPC协议版本 */
		private final String jsonrpc;
		/** 要调用的方法名 */
		private final String method;
		/** 方法参数 */
		private final Object params;

		/**
		 * 创建JSON-RPC通知消息
		 *
		 * @param jsonrpc JSON-RPC协议版本
		 * @param method 要调用的方法名
		 * @param params 方法参数
		 */
		public JSONRPCNotification(
				@JsonProperty("jsonrpc") String jsonrpc,
				@JsonProperty("method") String method,
				@JsonProperty("params") Object params) {
			this.jsonrpc = jsonrpc;
			this.method = method;
			this.params = params;
		}

		@Override
		public String getJsonrpc() {
			return jsonrpc;
		}

		public String getMethod() {
			return method;
		}

		public Object getParams() {
			return params;
		}
	}

	/**
	 * JSON-RPC响应消息
	 *
	 * <p>服务器对请求的响应，包含结果或错误信息
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCResponse implements JSONRPCMessage {
		/** JSON-RPC协议版本 */
		private final String jsonrpc;
		/** 关联的请求ID */
		private final Object id;
		/** 请求执行结果（成功时） */
		private final Object result;
		/** 错误信息（失败时） */
		private final JSONRPCError error;

		/**
		 * 创建JSON-RPC响应消息
		 *
		 * @param jsonrpc JSON-RPC协议版本
		 * @param id 关联的请求ID
		 * @param result 请求执行结果
		 * @param error 错误信息
		 */
		public JSONRPCResponse(
				@JsonProperty("jsonrpc") String jsonrpc,
				@JsonProperty("id") Object id,
				@JsonProperty("result") Object result,
				@JsonProperty("error") JSONRPCError error) {
			this.jsonrpc = jsonrpc;
			this.id = id;
			this.result = result;
			this.error = error;
		}

		@Override
		public String getJsonrpc() {
			return jsonrpc;
		}

		public Object getId() {
			return id;
		}

		public Object getResult() {
			return result;
		}

		public JSONRPCError getError() {
			return error;
		}

		/**
		 * JSON-RPC错误对象
		 *
		 * <p>包含错误码、错误消息和可选的附加数据
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class JSONRPCError {
			/** 错误码 */
			private final int code;
			/** 错误描述消息 */
			private final String message;
			/** 附加错误数据 */
			private final Object data;

			/**
			 * 创建JSON-RPC错误对象
			 *
			 * @param code 错误码
			 * @param message 错误描述
			 * @param data 附加数据
			 */
			public JSONRPCError(
					@JsonProperty("code") int code,
					@JsonProperty("message") String message,
					@JsonProperty("data") Object data) {
				this.code = code;
				this.message = message;
				this.data = data;
			}

			public int getCode() {
				return code;
			}

			public String getMessage() {
				return message;
			}

			public Object getData() {
				return data;
			}
		}
	}

	// ---------------------------
	// 初始化相关类
	// ---------------------------
	/**
	 * 初始化请求
	 *
	 * <p>客户端发送此请求以启动MCP会话，声明其协议版本和功能
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeRequest implements Request {
		/** 客户端支持的协议版本 */
		private final String protocolVersion;
		/** 客户端能力声明 */
		private final ClientCapabilities capabilities;
		/** 客户端实现信息 */
		private final Implementation clientInfo;

		/**
		 * 创建初始化请求
		 *
		 * @param protocolVersion 客户端支持的协议版本
		 * @param capabilities 客户端能力声明
		 * @param clientInfo 客户端实现信息
		 */
		public InitializeRequest(
				@JsonProperty("protocolVersion") String protocolVersion,
				@JsonProperty("capabilities") ClientCapabilities capabilities,
				@JsonProperty("clientInfo") Implementation clientInfo) {
			this.protocolVersion = protocolVersion;
			this.capabilities = capabilities;
			this.clientInfo = clientInfo;
		}

		public String getProtocolVersion() {
			return protocolVersion;
		}

		public ClientCapabilities getCapabilities() {
			return capabilities;
		}

		public Implementation getClientInfo() {
			return clientInfo;
		}
	}

	/**
	 * 初始化响应
	 *
	 * <p>服务器响应初始化请求，声明其协议版本和能力
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeResult implements Result {
		/** 服务器支持的协议版本 */
		private final String protocolVersion;
		/** 服务器能力声明 */
		private final ServerCapabilities capabilities;
		/** 服务器实现信息 */
		private final Implementation serverInfo;
		/** 服务器使用说明 */
		private final String instructions;

		/**
		 * 创建初始化响应
		 *
		 * @param protocolVersion 服务器支持的协议版本
		 * @param capabilities 服务器能力声明
		 * @param serverInfo 服务器实现信息
		 * @param instructions 服务器使用说明
		 */
		public InitializeResult(
				@JsonProperty("protocolVersion") String protocolVersion,
				@JsonProperty("capabilities") ServerCapabilities capabilities,
				@JsonProperty("serverInfo") Implementation serverInfo,
				@JsonProperty("instructions") String instructions) {
			this.protocolVersion = protocolVersion;
			this.capabilities = capabilities;
			this.serverInfo = serverInfo;
			this.instructions = instructions;
		}

		public String getProtocolVersion() {
			return protocolVersion;
		}

		public ServerCapabilities getCapabilities() {
			return capabilities;
		}

		public Implementation getServerInfo() {
			return serverInfo;
		}

		public String getInstructions() {
			return instructions;
		}
	}

	/**
	 * 客户端能力声明
	 *
	 * <p>客户端可以实现额外的功能来丰富连接的MCP服务器。
	 * 这些能力可用于扩展服务器功能，或向服务器提供有关客户端能力的额外信息。
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ClientCapabilities {

		/** 实验性功能键值对 */
		private final Map<String, Object> experimental;
		/** 根目录能力 */
		private final RootCapabilities roots;
		/** 采样能力 */
		private final Sampling sampling;
        /** 引导能力 */
        private final Elicitation elicitation;

		/**
		 * 创建客户端能力声明
		 *
		 * @param experimental 实验性功能
		 * @param roots 根目录能力
		 * @param sampling 采样能力
		 * @param elicitation 引导能力
		 */
		public ClientCapabilities(
				@JsonProperty("experimental") Map<String, Object> experimental,
				@JsonProperty("roots") RootCapabilities roots,
				@JsonProperty("sampling") Sampling sampling,
                @JsonProperty("elicitation") Elicitation elicitation) {
			this.experimental = experimental;
			this.roots = roots;
			this.sampling = sampling;
            this.elicitation = elicitation;
		}

		/**
		 * 根目录能力
		 *
		 * <p>根目录定义了服务器可以在文件系统中操作的范围，
		 * 允许它们了解可以访问哪些目录和文件。
		 * 服务器可以从支持的客户端请求根目录列表，
		 * 并在该列表更改时接收通知。
         */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class RootCapabilities {
			/** 是否支持列表变化通知 */
			private final Boolean listChanged;

			/**
			 * 创建根目录能力
			 *
			 * @param listChanged 是否支持列表变化通知
			 */
			public RootCapabilities(
					@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public Boolean getListChanged() {
				return listChanged;
			}
		}

		/**
		 * 采样能力
		 *
		 * <p>为服务器提供通过客户端从语言模型请求LLM采样
		 * （"完成"或"生成"）的标准化方式。
		 * 此流程允许客户端维护对模型访问、选择和权限的控制，
		 * 同时使服务器能够利用AI功能——无需服务器API密钥。
		 * 服务器可以请求基于文本或图像的交互，
		 * 并选择在其提示中包含来自MCP服务器的上下文。
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class Sampling {
		}

        /**
         * 引导能力
         *
         * <p>允许服务器向客户端请求结构化数据或用户交互
         */
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class Elicitation {
        }

		public Map<String, Object> getExperimental() {
			return experimental;
		}

		public RootCapabilities getRoots() {
			return roots;
		}

		public Sampling getSampling() {
			return sampling;
		}

        public Elicitation getElicitation() {
            return elicitation;
        }

		/**
		 * 创建Builder实例
		 * @return Builder构建器
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * 客户端能力构建器
		 */
		public static class Builder {
			/** 实验性功能 */
			private Map<String, Object> experimental;
			/** 根目录能力 */
			private RootCapabilities roots;
			/** 采样能力 */
			private Sampling sampling;
            /** 引导能力 */
            private Elicitation elicitation;

			/**
			 * 设置实验性功能
			 * @param experimental 实验性功能映射
			 * @return this
			 */
			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			/**
			 * 设置根目录能力
			 * @param listChanged 是否支持列表变化通知
			 * @return this
			 */
			public Builder roots(Boolean listChanged) {
				this.roots = new RootCapabilities(listChanged);
				return this;
			}

			/**
			 * 启用采样能力
			 * @return this
			 */
			public Builder sampling() {
				this.sampling = new Sampling();
				return this;
			}

            /**
             * 启用引导能力
             * @return this
             */
            public Builder elicitation() {
                this.elicitation = new Elicitation();
                return this;
            }

			/**
			 * 构建客户端能力对象
			 * @return 客户端能力实例
			 */
			public ClientCapabilities build() {
				return new ClientCapabilities(experimental, roots, sampling, elicitation);
			}
		}
	}

	/**
	 * 服务器能力声明
	 *
	 * <p>服务器声明其支持的功能和特性
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ServerCapabilities {
		/** 实验性功能键值对 */
		private final Map<String, Object> experimental;
		/** 日志能力 */
		private final LoggingCapabilities logging;
		/** 提示词能力 */
		private final PromptCapabilities prompts;
		/** 资源能力 */
		private final ResourceCapabilities resources;
		/** 工具能力 */
		private final ToolCapabilities tools;

		/**
		 * 创建服务器能力声明
		 *
		 * @param experimental 实验性功能
		 * @param logging 日志能力
		 * @param prompts 提示词能力
		 * @param resources 资源能力
		 * @param tools 工具能力
		 */
		public ServerCapabilities(
				@JsonProperty("experimental") Map<String, Object> experimental,
				@JsonProperty("logging") LoggingCapabilities logging,
				@JsonProperty("prompts") PromptCapabilities prompts,
				@JsonProperty("resources") ResourceCapabilities resources,
				@JsonProperty("tools") ToolCapabilities tools) {
			this.experimental = experimental;
			this.logging = logging;
			this.prompts = prompts;
			this.resources = resources;
			this.tools = tools;
		}

		/**
		 * 创建Builder实例
		 * @return Builder构建器
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * 服务器能力构建器
		 */
		public static class Builder {
			/** 实验性功能 */
			private Map<String, Object> experimental;
			/** 日志能力 */
			private LoggingCapabilities logging;
			/** 提示词能力 */
			private PromptCapabilities prompts;
			/** 资源能力 */
			private ResourceCapabilities resources;
			/** 工具能力 */
			private ToolCapabilities tools;

			/**
			 * 设置实验性功能
			 * @param experimental 实验性功能映射
			 * @return this
			 */
			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			/**
			 * 设置日志能力
			 * @param logging 日志能力
			 * @return this
			 */
			public Builder logging(LoggingCapabilities logging) {
				this.logging = logging;
				return this;
			}

			/**
			 * 设置提示词能力
			 * @param prompts 提示词能力
			 * @return this
			 */
			public Builder prompts(PromptCapabilities prompts) {
				this.prompts = prompts;
				return this;
			}

			/**
			 * 设置资源能力
			 * @param resources 资源能力
			 * @return this
			 */
			public Builder resources(ResourceCapabilities resources) {
				this.resources = resources;
				return this;
			}

			/**
			 * 设置工具能力
			 * @param tools 工具能力
			 * @return this
			 */
			public Builder tools(ToolCapabilities tools) {
				this.tools = tools;
				return this;
			}

			/**
			 * 构建服务器能力对象
			 * @return 服务器能力实例
			 */
			public ServerCapabilities build() {
				return new ServerCapabilities(experimental, logging, prompts, resources, tools);
			}
		}

		/**
		 * 日志能力
		 *
		 * <p>标记服务器支持日志功能
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class LoggingCapabilities {
		}

		/**
		 * 提示词能力
		 *
		 * <p>声明服务器对提示词的支持能力
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class PromptCapabilities {
			/** 是否支持提示词列表变化通知 */
			private final Boolean listChanged;

			/**
			 * 创建提示词能力
			 *
			 * @param listChanged 是否支持列表变化通知
			 */
			public PromptCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public Boolean getListChanged() {
				return listChanged;
			}
		}

		/**
		 * 资源能力
		 *
		 * <p>声明服务器对资源的支持能力
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ResourceCapabilities {
			/** 是否支持资源订阅 */
			private final Boolean subscribe;
			/** 是否支持资源列表变化通知 */
			private final Boolean listChanged;

			/**
			 * 创建资源能力
			 *
			 * @param subscribe 是否支持资源订阅
			 * @param listChanged 是否支持列表变化通知
			 */
			public ResourceCapabilities(
					@JsonProperty("subscribe") Boolean subscribe,
					@JsonProperty("listChanged") Boolean listChanged) {
				this.subscribe = subscribe;
				this.listChanged = listChanged;
			}

			public Boolean getSubscribe() {
				return subscribe;
			}

			public Boolean getListChanged() {
				return listChanged;
			}
		}

		/**
		 * 工具能力
		 *
		 * <p>声明服务器对工具的支持能力
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ToolCapabilities {
			/** 是否支持工具列表变化通知 */
			private final Boolean listChanged;

			/**
			 * 创建工具能力
			 *
			 * @param listChanged 是否支持列表变化通知
			 */
			public ToolCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public Boolean getListChanged() {
				return listChanged;
			}
		}

		public Map<String, Object> getExperimental() {
			return experimental;
		}

		public LoggingCapabilities getLogging() {
			return logging;
		}

		public PromptCapabilities getPrompts() {
			return prompts;
		}

		public ResourceCapabilities getResources() {
			return resources;
		}

		public ToolCapabilities getTools() {
			return tools;
		}
	}


	/**
	 * 实现信息
	 *
	 * <p>描述客户端或服务器的实现细节
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Implementation {
		/** 实现名称 */
		private final String name;
		/** 实现版本 */
		private final String version;

		/**
		 * 创建实现信息
		 *
		 * @param name 实现名称
		 * @param version 实现版本
		 */
		public Implementation(
				@JsonProperty("name") String name,
				@JsonProperty("version") String version) {
			this.name = name;
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}
	}

	// ===========================
	// 枚举和基础类型定义
	// ===========================

	/**
	 * 角色枚举
	 *
	 * <p>标识消息发送者的角色
	 */
	public enum Role {
		/** 用户角色 */
		@JsonProperty("user") USER,
		/** 助手角色 */
		@JsonProperty("assistant") ASSISTANT
	}

	/**
	 * 停止原因枚举
	 *
	 * <p>标识消息生成停止的原因
	 */
	public enum StopReason {
		/** 正常停止 */
		@JsonProperty("stop") STOP,
		/** 达到最大长度 */
		@JsonProperty("length") LENGTH,
		/** 内容过滤器触发 */
		@JsonProperty("content_filter") CONTENT_FILTER
	}

	/**
	 * 上下文包含策略枚举
	 *
	 * <p>定义如何在提示中包含上下文
	 */
	public enum ContextInclusionStrategy {
		/** 不包含任何上下文 */
		@JsonProperty("none") NONE,
		/** 包含所有上下文 */
		@JsonProperty("all") ALL,
		/** 仅包含相关上下文 */
		@JsonProperty("relevant") RELEVANT
	}

	// ---------------------------
	// 资源接口定义
	// ---------------------------
	/**
	 * 可注解接口
	 *
	 * <p>包含可选注解的对象的基类。
	 * 客户端可以使用注解来决定如何使用或显示对象
	 */
	public interface Annotated {

		/**
		 * 获取注解对象
		 * @return 注解对象
		 */
		Annotations annotations();

	}

	/**
	 * 注解类
	 *
	 * <p>客户端的可选注解。
	 * 客户端可以使用注解来决定如何使用或显示对象
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Annotations {
		/** 目标受众角色列表 */
		private final List<Role> audience;
		/** 优先级 */
		private final Double priority;

		/**
		 * 创建注解对象
		 *
		 * @param audience 目标受众
		 * @param priority 优先级
		 */
		public Annotations(
				@JsonProperty("audience") List<Role> audience,
				@JsonProperty("priority") Double priority) {
			this.audience = audience;
			this.priority = priority;
		}

		public List<Role> getAudience() {
			return audience;
		}

		public Double getPriority() {
			return priority;
		}
	}

	/**
	 * 资源类
	 *
	 * <p>服务器能够读取的已知资源
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Resource implements Annotated {
		/** 资源URI */
		private final String uri;
		/** 资源名称 */
		private final String name;
		/** 资源描述 */
		private final String description;
		/** MIME类型 */
		private final String mimeType;
		/** 注解信息 */
		private final Annotations annotations;

		/**
		 * 创建资源对象
		 *
		 * @param uri 资源URI
		 * @param name 资源名称
		 * @param description 资源描述
		 * @param mimeType MIME类型
		 * @param annotations 注解信息
		 */
		public Resource(
				@JsonProperty("uri") String uri,
				@JsonProperty("name") String name,
				@JsonProperty("description") String description,
				@JsonProperty("mimeType") String mimeType,
				@JsonProperty("annotations") Annotations annotations) {
			this.uri = uri;
			this.name = name;
			this.description = description;
			this.mimeType = mimeType;
			this.annotations = annotations;
		}

		@Override
		public Annotations annotations() {
			return annotations;
		}

		public String getUri() {
			return uri;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public String getMimeType() {
			return mimeType;
		}
	}

	/**
	 * 资源模板
	 *
	 * <p>资源模板允许服务器使用URI模板暴露参数化资源
	 *
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ResourceTemplate implements Annotated {
		/** URI模板字符串 */
		private final String uriTemplate;
		/** 模板名称 */
		private final String name;
		/** 模板描述 */
		private final String description;
		/** MIME类型 */
		private final String mimeType;
		/** 注解信息 */
		private final Annotations annotations;

		/**
		 * 创建资源模板对象
		 *
		 * @param uriTemplate URI模板字符串
		 * @param name 模板名称
		 * @param description 模板描述
		 * @param mimeType MIME类型
		 * @param annotations 注解信息
		 */
		public ResourceTemplate(
				@JsonProperty("uriTemplate") String uriTemplate,
				@JsonProperty("name") String name,
				@JsonProperty("description") String description,
				@JsonProperty("mimeType") String mimeType,
				@JsonProperty("annotations") Annotations annotations) {
			this.uriTemplate = uriTemplate;
			this.name = name;
			this.description = description;
			this.mimeType = mimeType;
			this.annotations = annotations;
		}

		@Override
		public Annotations annotations() {
			return annotations;
		}

		public String getUriTemplate() {
			return uriTemplate;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public String getMimeType() {
			return mimeType;
		}
	}

	/**
	 * 列出资源结果
	 *
	 * <p>服务器响应资源列表请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourcesResult implements Result {
		/** 资源列表 */
		private final List<Resource> resources;
		/** 下一页游标，用于分页 */
		private final String nextCursor;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建列出资源结果
		 *
		 * @param resources 资源列表
		 * @param nextCursor 下一页游标
		 * @param meta 元数据
		 */
		public ListResourcesResult(
				@JsonProperty("resources") List<Resource> resources,
				@JsonProperty("nextCursor") String nextCursor,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.resources = resources;
			this.nextCursor = nextCursor;
			this.meta = meta;
		}

		/**
		 * 创建列出资源结果（不含元数据）
		 *
		 * @param resources 资源列表
		 * @param nextCursor 下一页游标
		 */
		public ListResourcesResult(List<Resource> resources, String nextCursor) {
			this(resources, nextCursor, null);
		}

		public List<Resource> getResources() {
			return resources;
		}

		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		public Map<String, Object> getMeta() {
			return meta();
		}

		/**
		 * 获取进度令牌
		 * @return 进度令牌，如果未设置则返回null
		 */
		public Object getProgressToken() {
			return (meta() != null) ? meta().get("progressToken") : null;
		}
	}

	/**
	 * 列出资源模板结果
	 *
	 * <p>服务器响应资源模板列表请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourceTemplatesResult implements Result {
		/** 资源模板列表 */
		private final List<ResourceTemplate> resourceTemplates;
		/** 下一页游标，用于分页 */
		private final String nextCursor;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建列出资源模板结果
		 *
		 * @param resourceTemplates 资源模板列表
		 * @param nextCursor 下一页游标
		 * @param meta 元数据
		 */
		public ListResourceTemplatesResult(
				@JsonProperty("resourceTemplates") List<ResourceTemplate> resourceTemplates,
				@JsonProperty("nextCursor") String nextCursor,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.resourceTemplates = resourceTemplates;
			this.nextCursor = nextCursor;
			this.meta = meta;
		}

		/**
		 * 创建列出资源模板结果（不含元数据）
		 *
		 * @param resourceTemplates 资源模板列表
		 * @param nextCursor 下一页游标
		 */
		public ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates, String nextCursor) {
			this(resourceTemplates, nextCursor, null);
		}

		public List<ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		public Map<String, Object> getMeta() {
			return meta();
		}
	}

	/**
	 * 读取资源请求
	 *
	 * <p>客户端请求读取特定资源的内容
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceRequest {
		/** 资源URI */
		private final String uri;

		/**
		 * 创建读取资源请求
		 *
		 * @param uri 资源URI
		 */
		public ReadResourceRequest(
				@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	/**
	 * 读取资源结果
	 *
	 * <p>服务器响应资源读取请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceResult implements Result {
		/** 资源内容列表 */
		private final List<ResourceContents> contents;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建读取资源结果
		 *
		 * @param contents 资源内容列表
		 * @param meta 元数据
		 */
		public ReadResourceResult(
				@JsonProperty("contents") List<ResourceContents> contents,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.contents = contents;
			this.meta = meta;
		}

		/**
		 * 创建读取资源结果（不含元数据）
		 *
		 * @param contents 资源内容列表
		 */
		public ReadResourceResult(List<ResourceContents> contents) {
			this(contents, null);
		}

		public List<ResourceContents> getContents() {
			return contents;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		public Map<String, Object> getMeta() {
			return meta();
		}
	}

	/**
	 * 订阅请求
	 *
	 * <p>客户端发送此请求以订阅特定资源的变化通知
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SubscribeRequest {
		/** 资源URI */
		private final String uri;

		/**
		 * 创建订阅请求
		 *
		 * @param uri 资源URI
		 */
		public SubscribeRequest(
				@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	/**
	 * 取消订阅请求
	 *
	 * <p>客户端发送此请求以取消订阅特定资源的变化通知
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UnsubscribeRequest {
		/** 资源URI */
		private final String uri;

		/**
		 * 创建取消订阅请求
		 *
		 * @param uri 资源URI
		 */
		public UnsubscribeRequest(
				@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	/**
	 * 资源内容接口
	 *
	 * <p>表示特定资源或子资源的内容
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
			@JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
	public interface ResourceContents {

		/**
		 * 获取此资源的URI
		 * @return 资源URI
		 */
		String uri();

		/**
		 * 获取此资源的MIME类型
		 * @return MIME类型
		 */
		String mimeType();

	}

	/**
	 * 文本资源内容
	 *
	 * <p>表示资源的文本内容
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextResourceContents implements ResourceContents {
		/** 资源URI */
		private final String uri;
		/** MIME类型 */
		private final String mimeType;
		/** 文本内容 */
		private final String text;

		/**
		 * 创建文本资源内容
		 *
		 * @param uri 资源URI
		 * @param mimeType MIME类型
		 * @param text 文本内容
		 */
		public TextResourceContents(
				@JsonProperty("uri") String uri,
				@JsonProperty("mimeType") String mimeType,
				@JsonProperty("text") String text) {
			this.uri = uri;
			this.mimeType = mimeType;
			this.text = text;
		}

		@Override
		public String uri() {
			return uri;
		}

		@Override
		public String mimeType() {
			return mimeType;
		}

		public String getText() {
			return text;
		}
	}

	/**
	 * 二进制资源内容
	 *
	 * <p>表示资源的二进制内容。
	 * 仅当资源实际上可以表示为二进制数据（而非文本）时才应设置此字段
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BlobResourceContents implements ResourceContents {
		/** 资源URI */
		private final String uri;
		/** MIME类型 */
		private final String mimeType;
		/** 二进制数据（Base64编码） */
		private final String blob;

		/**
		 * 创建二进制资源内容
		 *
		 * @param uri 资源URI
		 * @param mimeType MIME类型
		 * @param blob 二进制数据
		 */
		public BlobResourceContents(
				@JsonProperty("uri") String uri,
				@JsonProperty("mimeType") String mimeType,
				@JsonProperty("blob") String blob) {
			this.uri = uri;
			this.mimeType = mimeType;
			this.blob = blob;
		}

		@Override
		public String uri() {
			return uri;
		}

		@Override
		public String mimeType() {
			return mimeType;
		}

		public String getBlob() {
			return blob;
		}
	}

	// ---------------------------
	// 提示词接口定义
	// ---------------------------
	/**
	 * 提示词类
	 *
	 * <p>服务器提供的提示词或提示词模板
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Prompt {
		/** 提示词名称 */
		private final String name;
		/** 提示词描述 */
		private final String description;
		/** 提示词参数列表 */
		private final List<PromptArgument> arguments;

		/**
		 * 创建提示词对象
		 *
		 * @param name 提示词名称
		 * @param description 提示词描述
		 * @param arguments 提示词参数列表
		 */
		public Prompt(
				@JsonProperty("name") String name,
				@JsonProperty("description") String description,
				@JsonProperty("arguments") List<PromptArgument> arguments) {
			this.name = name;
			this.description = description;
			this.arguments = arguments;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public List<PromptArgument> getArguments() {
			return arguments;
		}
	}

	/**
	 * 提示词参数类
	 *
	 * <p>描述提示词可以接受的参数
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptArgument {
		/** 参数名称 */
		private final String name;
		/** 参数描述 */
		private final String description;
		/** 是否必需参数 */
		private final Boolean required;

		/**
		 * 创建提示词参数对象
		 *
		 * @param name 参数名称
		 * @param description 参数描述
		 * @param required 是否必需
		 */
		public PromptArgument(
				@JsonProperty("name") String name,
				@JsonProperty("description") String description,
				@JsonProperty("required") Boolean required) {
			this.name = name;
			this.description = description;
			this.required = required;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public Boolean getRequired() {
			return required;
		}
	}

	/**
	 * 提示词消息类
	 *
	 * <p>描述作为提示词一部分返回的消息。
	 * 这与SamplingMessage类似，但也支持嵌入来自MCP服务器的资源
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptMessage {
		/** 消息角色 */
		private final Role role;
		/** 消息内容 */
		private final Content content;

		/**
		 * 创建提示词消息对象
		 *
		 * @param role 消息角色
		 * @param content 消息内容
		 */
		public PromptMessage(
				@JsonProperty("role") Role role,
				@JsonProperty("content") Content content) {
			this.role = role;
			this.content = content;
		}

		public Role getRole() {
			return role;
		}

		public Content getContent() {
			return content;
		}
	}

	/**
	 * 列出提示词结果
	 *
	 * <p>服务器响应提示词列表请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListPromptsResult implements Result {
		/** 提示词列表 */
		private final List<Prompt> prompts;
		/** 下一页游标，用于分页 */
		private final String nextCursor;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建列出提示词结果
		 *
		 * @param prompts 提示词列表
		 * @param nextCursor 下一页游标
		 * @param meta 元数据
		 */
		public ListPromptsResult(
				@JsonProperty("prompts") List<Prompt> prompts,
				@JsonProperty("nextCursor") String nextCursor,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.prompts = prompts;
			this.nextCursor = nextCursor;
			this.meta = meta;
		}

		/**
		 * 创建列出提示词结果（不含元数据）
		 *
		 * @param prompts 提示词列表
		 * @param nextCursor 下一页游标
		 */
		public ListPromptsResult(List<Prompt> prompts, String nextCursor) {
			this(prompts, nextCursor, null);
		}

		public List<Prompt> getPrompts() {
			return prompts;
		}

		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		public Map<String, Object> getMeta() {
			return meta();
		}
	}

	/**
	 * 获取提示词请求
	 *
	 * <p>客户端使用此请求获取服务器提供的特定提示词
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptRequest implements Request {
		/** 提示词名称 */
		private final String name;
		/** 提示词参数 */
		private final Map<String, Object> arguments;

		/**
		 * 创建获取提示词请求
		 *
		 * @param name 提示词名称
		 * @param arguments 提示词参数
		 */
		public GetPromptRequest(
				@JsonProperty("name") String name,
				@JsonProperty("arguments") Map<String, Object> arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		public String getName() {
			return name;
		}

		public Map<String, Object> getArguments() {
			return arguments;
		}
	}

	/**
	 * 获取提示词结果
	 *
	 * <p>服务器响应获取提示词请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptResult implements Result {
		/** 提示词描述 */
		private final String description;
		/** 提示词消息列表 */
		private final List<PromptMessage> messages;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建获取提示词结果
		 *
		 * @param description 提示词描述
		 * @param messages 提示词消息列表
		 * @param meta 元数据
		 */
		public GetPromptResult(
				@JsonProperty("description") String description,
				@JsonProperty("messages") List<PromptMessage> messages,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.description = description;
			this.messages = messages;
			this.meta = meta;
		}

		/**
		 * 创建获取提示词结果（不含元数据）
		 *
		 * @param description 提示词描述
		 * @param messages 提示词消息列表
		 */
		public GetPromptResult(String description, List<PromptMessage> messages) {
			this(description, messages, null);
		}

		public String getDescription() {
			return description;
		}

		public List<PromptMessage> getMessages() {
			return messages;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		public Map<String, Object> getMeta() {
			return meta();
		}
	}

	// ---------------------------
	// 工具接口定义
	// ---------------------------
	/**
	 * 列出工具结果
	 *
	 * <p>服务器响应工具列表请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListToolsResult implements Result {
		/** 工具列表 */
		private final List<Tool> tools;
		/** 下一页游标，用于分页 */
		private final String nextCursor;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建列出工具结果
		 *
		 * @param tools 工具列表
		 * @param nextCursor 下一页游标
		 * @param meta 元数据
		 */
		public ListToolsResult(
				@JsonProperty("tools") List<Tool> tools,
				@JsonProperty("nextCursor") String nextCursor,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.tools = tools;
			this.nextCursor = nextCursor;
			this.meta = meta;
		}

		/**
		 * 创建列出工具结果（不含元数据）
		 *
		 * @param tools 工具列表
		 * @param nextCursor 下一页游标
		 */
		public ListToolsResult(List<Tool> tools, String nextCursor) {
			this(tools, nextCursor, null);
		}

		public List<Tool> getTools() {
			return tools;
		}

		public String getNextCursor() {
			return nextCursor;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		public Map<String, Object> getMeta() {
			return meta();
		}
	}

	/**
	 * JSON Schema类
	 *
	 * <p>描述工具输入参数的JSON Schema
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JsonSchema {
		/** 类型 */
		private final String type;
		/** 属性映射 */
		private final Map<String, Object> properties;
		/** 必需字段列表 */
		private final List<String> required;
		/** 是否允许额外属性 */
		private final Boolean additionalProperties;

		/**
		 * 创建JSON Schema对象
		 *
		 * @param type 类型
		 * @param properties 属性映射
		 * @param required 必需字段列表
		 * @param additionalProperties 是否允许额外属性
		 */
		public JsonSchema(
				@JsonProperty("type") String type,
				@JsonProperty("properties") Map<String, Object> properties,
				@JsonProperty("required") List<String> required,
				@JsonProperty("additionalProperties") Boolean additionalProperties) {
			this.type = type;
			this.properties = properties;
			this.required = required;
			this.additionalProperties = additionalProperties;
		}

		public String getType() {
			return type;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public List<String> getRequired() {
			return required;
		}

		public Boolean getAdditionalProperties() {
			return additionalProperties;
		}
	}

	/**
	 * 工具类
	 *
	 * <p>表示服务器提供的工具。工具使服务器能够向系统暴露可执行功能。
	 * 通过这些工具，你可以与外部系统交互、执行计算并在现实世界中采取行动
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Tool {
		/** 工具名称 */
		private final String name;
		/** 工具描述 */
		private final String description;
		/** 输入参数的JSON Schema */
		private final JsonSchema inputSchema;

		/**
		 * 创建工具对象
		 *
		 * @param name 工具名称
		 * @param description 工具描述
		 * @param inputSchema 输入参数的JSON Schema
		 */
		public Tool(
				@JsonProperty("name") String name,
				@JsonProperty("description") String description,
				@JsonProperty("inputSchema") JsonSchema inputSchema) {
			this.name = name;
			this.description = description;
			this.inputSchema = inputSchema;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public JsonSchema getInputSchema() {
			return inputSchema;
		}
	}

	/**
	 * 解析JSON Schema字符串
	 *
	 * @param schema JSON Schema字符串
	 * @return JsonSchema对象
	 * @throws IllegalArgumentException 如果schema格式无效
	 */
	private static JsonSchema parseSchema(String schema) {
		try {
			// 使用ObjectMapper将JSON字符串解析为JsonSchema对象
			return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
		}
		catch (IOException e) {
			// 解析失败，抛出包含详细信息的异常
			throw new IllegalArgumentException("Invalid schema: " + schema, e);
		}
	}

	/**
	 * 调用工具请求
	 *
	 * <p>客户端使用此请求调用服务器提供的工具
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolRequest implements Request {
		/** 工具名称 */
		private final String name;
		/** 工具参数 */
		private final Map<String, Object> arguments;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建调用工具请求
		 *
		 * @param name 工具名称
		 * @param arguments 工具参数
		 * @param meta 元数据
		 */
		public CallToolRequest(
				@JsonProperty("name") String name,
				@JsonProperty("arguments") Map<String, Object> arguments,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.name = name;
			this.arguments = arguments;
			this.meta = meta;
		}

		/**
		 * 解析JSON参数字符串
		 *
		 * @param jsonArguments JSON格式的参数字符串
		 * @return 参数映射
		 * @throws IllegalArgumentException 如果参数格式无效
		 */
		private static Map<String, Object> parseJsonArguments(String jsonArguments) {
			try {
				// 将JSON字符串解析为Map
				return OBJECT_MAPPER.readValue(jsonArguments, MAP_TYPE_REF);
			}
			catch (IOException e) {
				// 解析失败，抛出异常
				throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
			}
		}

		public String getName() {
			return name;
		}

		public Map<String, Object> getArguments() {
			return arguments;
		}

		@Override
		public Map<String, Object> meta() {
			return meta;
		}

		/**
		 * 创建Builder实例
		 * @return Builder构建器
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * 调用工具请求构建器
		 */
		public static class Builder {

			/** 工具名称 */
			private String name;

			/** 工具参数 */
			private Map<String, Object> arguments;

			/** 元数据 */
			private Map<String, Object> meta;

			/**
			 * 设置工具名称
			 * @param name 工具名称
			 * @return this
			 */
			public Builder name(String name) {
				this.name = name;
				return this;
			}

			/**
			 * 设置工具参数（Map形式）
			 * @param arguments 参数映射
			 * @return this
			 */
			public Builder arguments(Map<String, Object> arguments) {
				this.arguments = arguments;
				return this;
			}

			/**
			 * 设置工具参数（JSON字符串形式）
			 * @param jsonArguments JSON格式的参数字符串
			 * @return this
			 */
			public Builder arguments(String jsonArguments) {
				this.arguments = parseJsonArguments(jsonArguments);
				return this;
			}

			/**
			 * 设置元数据
			 * @param meta 元数据
			 * @return this
			 */
			public Builder meta(Map<String, Object> meta) {
				this.meta = meta;
				return this;
			}

			/**
			 * 设置进度令牌
			 * @param progressToken 进度令牌
			 * @return this
			 */
			public Builder progressToken(String progressToken) {
				// 如果meta不存在，创建一个新的HashMap
				if (this.meta == null) {
					this.meta = new HashMap<>();
				}
				// 将进度令牌放入meta中
				this.meta.put("progressToken", progressToken);
				return this;
			}

			/**
			 * 构建调用工具请求对象
			 * @return CallToolRequest实例
			 * @throws IllegalArgumentException 如果名称为空
			 */
			public CallToolRequest build() {
				Assert.hasText(name, "name must not be empty");
				return new CallToolRequest(name, arguments, meta);
			}
		}
	}

	/**
	 * 调用工具结果
	 *
	 * <p>服务器响应工具调用请求的返回结果
	 */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallToolResult implements Result {
        /** 工具执行返回的内容列表 */
        private final List<Content> content;
        /** 是否为错误结果 */
        private final Boolean isError;
        /** 元数据 */
        private final Map<String, Object> meta;

        /**
         * 创建调用工具结果
         *
         * @param content 内容列表
         * @param isError 是否为错误
         * @param meta 元数据
         */
        public CallToolResult(
                @JsonProperty("content") List<Content> content,
                @JsonProperty("isError") Boolean isError,
                @JsonProperty("_meta") Map<String, Object> meta) {
            this.content = content;
            this.isError = isError;
            this.meta = meta;
        }

        /**
         * 创建调用工具结果（简化版本，使用文本内容）
         *
         * @param content 文本内容
         * @param isError 是否为错误
         * @param meta 元数据
         */
        public CallToolResult(String content, Boolean isError, Map<String, Object> meta) {
            // 将文本内容包装成TextContent对象并放入列表
            this(Collections.singletonList(new TextContent(content)), isError, meta);
        }

        public List<Content> getContent() {
            return content;
        }

        public Boolean getIsError() {
            return isError;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public Map<String, Object> getMeta() {
            return meta();
        }

        /**
         * 创建Builder实例
         * @return Builder构建器
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 调用工具结果构建器
         */
        public static class Builder {
            /** 内容列表，初始化为空列表 */
            private List<Content> content = new ArrayList<>();
            /** 是否为错误 */
            private Boolean isError;
            /** 元数据 */
            private Map<String, Object> meta;

            /**
             * 设置内容列表
             * @param content 内容列表
             * @return this
             */
            public Builder content(List<Content> content) {
                Assert.notNull(content, "content must not be null");
                this.content = content;
                return this;
            }

            /**
             * 设置文本内容列表
             * @param textContent 文本内容列表
             * @return this
             */
            public Builder textContent(List<String> textContent) {
                Assert.notNull(textContent, "textContent must not be null");
                // 将每个文本字符串转换为TextContent对象并添加到列表
                textContent.stream()
                        .map(TextContent::new)
                        .forEach(this.content::add);
                return this;
            }

            /**
             * 添加单个内容项
             * @param contentItem 内容项
             * @return this
             */
            public Builder addContent(Content contentItem) {
                Assert.notNull(contentItem, "contentItem must not be null");
                // 如果内容列表为null，创建新列表
                if (this.content == null) {
                    this.content = new ArrayList<>();
                }
                this.content.add(contentItem);
                return this;
            }

            /**
             * 添加文本内容
             * @param text 文本内容
             * @return this
             */
            public Builder addTextContent(String text) {
                Assert.notNull(text, "text must not be null");
                // 将文本包装成TextContent并添加
                return addContent(new TextContent(text));
            }

            /**
             * 设置是否为错误
             * @param isError 是否为错误
             * @return this
             */
            public Builder isError(Boolean isError) {
                Assert.notNull(isError, "isError must not be null");
                this.isError = isError;
                return this;
            }

            /**
             * 设置元数据
             * @param meta 元数据
             * @return this
             */
            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            /**
             * 构建调用工具结果对象
             * @return CallToolResult实例
             */
            public CallToolResult build() {
                return new CallToolResult(content, isError, meta);
            }
        }
    }


    // ---------------------------
	// 采样接口定义
	// ---------------------------
	/**
	 * 模型偏好设置
	 *
	 * <p>定义LLM模型的选择偏好和优先级
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelPreferences {
		/** 模型提示列表 */
		private final List<ModelHint> hints;
		/** 成本优先级 */
		private final Double costPriority;
		/** 速度优先级 */
		private final Double speedPriority;
		/** 智能优先级 */
		private final Double intelligencePriority;

		/**
		 * 创建模型偏好设置
		 *
		 * @param hints 模型提示列表
		 * @param costPriority 成本优先级
		 * @param speedPriority 速度优先级
		 * @param intelligencePriority 智能优先级
		 */
		public ModelPreferences(
				@JsonProperty("hints") List<ModelHint> hints,
				@JsonProperty("costPriority") Double costPriority,
				@JsonProperty("speedPriority") Double speedPriority,
				@JsonProperty("intelligencePriority") Double intelligencePriority) {
			this.hints = hints;
			this.costPriority = costPriority;
			this.speedPriority = speedPriority;
			this.intelligencePriority = intelligencePriority;
		}

		public List<ModelHint> getHints() {
			return hints;
		}

		public Double getCostPriority() {
			return costPriority;
		}

		public Double getSpeedPriority() {
			return speedPriority;
		}

		public Double getIntelligencePriority() {
			return intelligencePriority;
		}
	}

	/**
	 * 模型提示
	 *
	 * <p>用于指定特定的模型名称或特征
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelHint {
		/** 模型名称 */
		private final String name;

		/**
		 * 创建模型提示
		 *
		 * @param name 模型名称
		 */
		public ModelHint(
				@JsonProperty("name") String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * 采样消息
	 *
	 * <p>用于LLM采样的消息
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SamplingMessage {
		/** 消息角色 */
		private final Role role;
		/** 消息内容 */
		private final Content content;

		/**
		 * 创建采样消息
		 *
		 * @param role 消息角色
		 * @param content 消息内容
		 */
		public SamplingMessage(
				@JsonProperty("role") Role role,
				@JsonProperty("content") Content content) {
			this.role = role;
			this.content = content;
		}

		public Role getRole() {
			return role;
		}

		public Content getContent() {
			return content;
		}
	}

	/**
	 * 创建消息请求
	 *
	 * <p>用于请求LLM生成响应（采样）
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageRequest implements Request {
		/** 消息列表 */
		private final List<SamplingMessage> messages;
		/** 模型偏好设置 */
		private final ModelPreferences modelPreferences;
		/** 系统提示词 */
		private final String systemPrompt;
		/** 上下文包含策略 */
		private final ContextInclusionStrategy includeContext;
		/** 温度参数，控制随机性 */
		private final Double temperature;
		/** 最大token数 */
		private final int maxTokens;
		/** 停止序列列表 */
		private final List<String> stopSequences;
		/** 元数据 */
		private final Map<String, Object> meta;

		/**
		 * 创建消息请求
		 *
		 * @param messages 消息列表
		 * @param modelPreferences 模型偏好设置
		 * @param systemPrompt 系统提示词
		 * @param includeContext 上下文包含策略
		 * @param temperature 温度参数
		 * @param maxTokens 最大token数
		 * @param stopSequences 停止序列
		 * @param meta 元数据
		 */
		public CreateMessageRequest(
				@JsonProperty("messages") List<SamplingMessage> messages,
				@JsonProperty("modelPreferences") ModelPreferences modelPreferences,
				@JsonProperty("systemPrompt") String systemPrompt,
				@JsonProperty("includeContext") ContextInclusionStrategy includeContext,
				@JsonProperty("temperature") Double temperature,
				@JsonProperty("maxTokens") int maxTokens,
				@JsonProperty("stopSequences") List<String> stopSequences,
                @JsonProperty("_meta") Map<String, Object> meta) {
			this.messages = messages;
			this.modelPreferences = modelPreferences;
			this.systemPrompt = systemPrompt;
			this.includeContext = includeContext;
			this.temperature = temperature;
			this.maxTokens = maxTokens;
			this.stopSequences = stopSequences;
			this.meta = meta;
		}

		public List<SamplingMessage> getMessages() {
			return messages;
		}

		public ModelPreferences getModelPreferences() {
			return modelPreferences;
		}

		public String getSystemPrompt() {
			return systemPrompt;
		}

		public ContextInclusionStrategy getIncludeContext() {
			return includeContext;
		}

		public Double getTemperature() {
			return temperature;
		}

		public int getMaxTokens() {
			return maxTokens;
		}

		public List<String> getStopSequences() {
			return stopSequences;
		}

        @Override
        public Map<String, Object> meta() {
            return meta;
        }
	}

	/**
	 * 创建消息结果
	 *
	 * <p>LLM采样请求的响应结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageResult implements Result {
		/** 响应角色 */
		private final Role role;
		/** 响应内容 */
		private final Content content;
		/** 使用的模型名称 */
		private final String model;
		/** 停止原因 */
		private final StopReason stopReason;

		/**
		 * 创建消息结果
		 *
		 * @param role 响应角色
		 * @param content 响应内容
		 * @param model 模型名称
		 * @param stopReason 停止原因
		 */
		public CreateMessageResult(
				@JsonProperty("role") Role role,
				@JsonProperty("content") Content content,
				@JsonProperty("model") String model,
				@JsonProperty("stopReason") StopReason stopReason) {
			this.role = role;
			this.content = content;
			this.model = model;
			this.stopReason = stopReason;
		}

		public Role getRole() {
			return role;
		}

		public Content getContent() {
			return content;
		}

		public String getModel() {
			return model;
		}

		public StopReason getStopReason() {
			return stopReason;
		}
	}

    // ===========================
    // 引导(Elicitation)相关类
    // ===========================
    /**
     * 引导请求
     *
     * <p>用于向客户端请求结构化数据或用户交互
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ElicitRequest implements Request {

        /** 引导消息 */
        private final String message;
        /** 请求的模式定义 */
        private final Map<String, Object> requestedSchema;
        /** 元数据 */
        private final Map<String, Object> meta;

        /**
         * 构造函数
         *
         * @param message 引导消息
         * @param requestedSchema 请求的模式
         * @param meta 元数据
         */
        public ElicitRequest(
                @JsonProperty("message") String message,
                @JsonProperty("requestedSchema") Map<String, Object> requestedSchema,
                @JsonProperty("_meta") Map<String, Object> meta) {
            this.message = message;
            this.requestedSchema = requestedSchema;
            this.meta = meta;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getRequestedSchema() {
            return requestedSchema;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public Map<String, Object> getMeta() {
            return meta();
        }

        /**
         * 向后兼容的构造函数（不含元数据）
         *
         * @param message 引导消息
         * @param requestedSchema 请求的模式
         */
        public ElicitRequest(String message, Map<String, Object> requestedSchema) {
            this(message, requestedSchema, null);
        }

        /**
         * 创建Builder实例
         * @return Builder构建器
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 引导请求构建器
         */
        public static class Builder {

            /** 引导消息 */
            private String message;
            /** 请求的模式 */
            private Map<String, Object> requestedSchema;
            /** 元数据 */
            private Map<String, Object> meta;

            /**
             * 设置引导消息
             * @param message 引导消息
             * @return this
             */
            public Builder message(String message) {
                this.message = message;
                return this;
            }

            /**
             * 设置请求的模式
             * @param requestedSchema 请求的模式
             * @return this
             */
            public Builder requestedSchema(Map<String, Object> requestedSchema) {
                this.requestedSchema = requestedSchema;
                return this;
            }

            /**
             * 设置元数据
             * @param meta 元数据
             * @return this
             */
            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            /**
             * 设置进度令牌
             * @param progressToken 进度令牌
             * @return this
             */
            public Builder progressToken(Object progressToken) {
                // 如果meta不存在，创建新HashMap
                if (this.meta == null) {
                    this.meta = new HashMap<>();
                }
                // 将进度令牌放入meta
                this.meta.put("progressToken", progressToken);
                return this;
            }

            /**
             * 构建引导请求对象
             * @return ElicitRequest实例
             */
            public ElicitRequest build() {
                return new ElicitRequest(message, requestedSchema, meta);
            }
        }
    }

    /**
     * 引导结果
     *
     * <p>引导请求的响应结果
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ElicitResult implements Result {

        /** 操作类型 */
        private final Action action;
        /** 内容数据 */
        private final Map<String, Object> content;
        /** 元数据 */
        private final Map<String, Object> meta;

        /**
         * 操作类型枚举
         */
        public enum Action {
            /** 接受 */
            @JsonProperty("accept") ACCEPT,
            /** 拒绝 */
            @JsonProperty("decline") DECLINE,
            /** 取消 */
            @JsonProperty("cancel") CANCEL
        }

        /**
         * 构造函数
         *
         * @param action 操作类型
         * @param content 内容数据
         * @param meta 元数据
         */
        public ElicitResult(
                @JsonProperty("action") Action action,
                @JsonProperty("content") Map<String, Object> content,
                @JsonProperty("_meta") Map<String, Object> meta) {
            this.action = action;
            this.content = content;
            this.meta = meta;
        }

        public Action getAction() {
            return action;
        }

        public Map<String, Object> getContent() {
            return content;
        }

        @Override
        public Map<String, Object> meta() {
            return meta;
        }

        public Map<String, Object> getMeta() {
            return meta();
        }

        /**
         * 向后兼容的构造函数（不含元数据）
         *
         * @param action 操作类型
         * @param content 内容数据
         */
        public ElicitResult(Action action, Map<String, Object> content) {
            this(action, content, null);
        }

        /**
         * 创建Builder实例
         * @return Builder构建器
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 引导结果构建器
         */
        public static class Builder {

            /** 操作类型 */
            private Action action;
            /** 内容数据 */
            private Map<String, Object> content;
            /** 元数据 */
            private Map<String, Object> meta;

            /**
             * 设置操作类型
             * @param action 操作类型
             * @return this
             */
            public Builder action(Action action) {
                this.action = action;
                return this;
            }

            /**
             * 设置内容数据
             * @param content 内容数据
             * @return this
             */
            public Builder content(Map<String, Object> content) {
                this.content = content;
                return this;
            }

            /**
             * 设置元数据
             * @param meta 元数据
             * @return this
             */
            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            /**
             * 构建引导结果对象
             * @return ElicitResult实例
             */
            public ElicitResult build() {
                return new ElicitResult(action, content, meta);
            }
        }
    }


    // ---------------------------
	// 分页接口定义
	// ---------------------------
	/**
	 * 分页请求
	 *
	 * <p>用于请求分页数据的基类
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedRequest {
		/** 当前游标，标识分页位置 */
		private final String cursor;

		/**
		 * 创建分页请求
		 *
		 * @param cursor 当前游标
		 */
		public PaginatedRequest(
				@JsonProperty("cursor") String cursor) {
			this.cursor = cursor;
		}

		public String getCursor() {
			return cursor;
		}
	}

	/**
	 * 分页结果
	 *
	 * <p>分页数据请求的响应基类
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedResult {
		/** 下一页游标，为null表示没有更多数据 */
		private final String nextCursor;

		/**
		 * 创建分页结果
		 *
		 * @param nextCursor 下一页游标
		 */
		public PaginatedResult(
				@JsonProperty("nextCursor") String nextCursor) {
			this.nextCursor = nextCursor;
		}

		public String getNextCursor() {
			return nextCursor;
		}
	}

	// ---------------------------
	// 进度和日志
	// ---------------------------
	/**
	 * 进度通知
	 *
	 * <p>用于报告长时间运行操作的进度
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProgressNotification {
		/** 进度令牌，用于关联请求 */
		private final String progressToken;
		/** 当前进度值 */
		private final double progress;
		/** 总量（可选） */
		private final Double total;

		/**
		 * 创建进度通知
		 *
		 * @param progressToken 进度令牌
		 * @param progress 当前进度
		 * @param total 总量
		 */
		public ProgressNotification(
				@JsonProperty("progressToken") String progressToken,
				@JsonProperty("progress") double progress,
				@JsonProperty("total") Double total) {
			this.progressToken = progressToken;
			this.progress = progress;
			this.total = total;
		}

		public String getProgressToken() {
			return progressToken;
		}

		public double getProgress() {
			return progress;
		}

		public Double getTotal() {
			return total;
		}
	}

	/**
	 * 日志消息通知
	 *
	 * <p>MCP提供了一种标准化的方式，让服务器向客户端发送结构化日志消息。
	 * 客户端可以通过设置最低日志级别来控制日志详细程度，
	 * 服务器发送包含严重性级别、可选记录器名称和任意可JSON序列化数据的通知
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LoggingMessageNotification {
		/** 日志级别 */
		private final LoggingLevel level;
		/** 记录器名称 */
		private final String logger;
		/** 日志数据 */
		private final Object data;

		/**
		 * 创建日志消息通知
		 *
		 * @param level 日志级别
		 * @param logger 记录器名称
		 * @param data 日志数据
		 */
		public LoggingMessageNotification(
				@JsonProperty("level") LoggingLevel level,
				@JsonProperty("logger") String logger,
				@JsonProperty("data") Object data) {
			this.level = level;
			this.logger = logger;
			this.data = data;
		}

		public LoggingLevel getLevel() {
			return level;
		}

		public String getLogger() {
			return logger;
		}

		public Object getData() {
			return data;
		}

		/**
		 * 创建Builder实例
		 * @return Builder构建器
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * 日志消息通知构建器
		 */
		public static class Builder {
			/** 日志级别，默认为INFO */
			private LoggingLevel level = LoggingLevel.INFO;
			/** 记录器名称，默认为"server" */
			private String logger = "server";
			/** 日志数据 */
			private Object data;

			/**
			 * 设置日志级别
			 * @param level 日志级别
			 * @return this
			 */
			public Builder level(LoggingLevel level) {
				this.level = level;
				return this;
			}

			/**
			 * 设置记录器名称
			 * @param logger 记录器名称
			 * @return this
			 */
			public Builder logger(String logger) {
				this.logger = logger;
				return this;
			}

			/**
			 * 设置日志数据
			 * @param data 日志数据
			 * @return this
			 */
			public Builder data(Object data) {
				this.data = data;
				return this;
			}

			/**
			 * 构建日志消息通知对象
			 * @return LoggingMessageNotification实例
			 */
			public LoggingMessageNotification build() {
				return new LoggingMessageNotification(level, logger, data);
			}
		}
	}

	/**
	 * 日志级别枚举
	 *
	 * <p>定义从低到高的日志级别
	 */
	public enum LoggingLevel {
		/** 调试级别 */
		@JsonProperty("debug") DEBUG(0),
		/** 信息级别 */
		@JsonProperty("info") INFO(1),
		/** 通知级别 */
		@JsonProperty("notice") NOTICE(2),
		/** 警告级别 */
		@JsonProperty("warning") WARNING(3),
		/** 错误级别 */
		@JsonProperty("error") ERROR(4),
		/** 严重级别 */
		@JsonProperty("critical") CRITICAL(5),
		/** 警报级别 */
		@JsonProperty("alert") ALERT(6),
		/** 紧急级别 */
		@JsonProperty("emergency") EMERGENCY(7);

		/** 级别数值 */
		private final int level;

		/**
		 * 构造日志级别
		 * @param level 级别数值
		 */
		LoggingLevel(int level) {
			this.level = level;
		}

		/**
		 * 获取级别数值
		 * @return 级别数值
		 */
		public int level() {
			return level;
		}
	}

	/**
	 * 设置日志级别请求
	 *
	 * <p>客户端使用此请求设置服务器的日志级别
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SetLevelRequest {
		/** 日志级别 */
		private final LoggingLevel level;

		/**
		 * 创建设置日志级别请求
		 *
		 * @param level 日志级别
		 */
		public SetLevelRequest(
				@JsonProperty("level") LoggingLevel level) {
			this.level = level;
		}

		public LoggingLevel getLevel() {
			return level;
		}
	}

	/**
	 * 中间结果通知
	 *
	 * <p>用于在流式工具执行期间发送中间结果。
	 * 这允许工具实时向客户端发送部分结果
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class IntermediateResultNotification {
		/** 结果类型 */
		private final String type;
		/** 结果数据 */
		private final Object data;

		/**
		 * 创建中间结果通知
		 *
		 * @param type 结果类型
		 * @param data 结果数据
		 */
		public IntermediateResultNotification(
				@JsonProperty("type") String type,
				@JsonProperty("data") Object data) {
			this.type = type;
			this.data = data;
		}

		public String getType() {
			return type;
		}

		public Object getData() {
			return data;
		}

	}

	// ---------------------------
	// 自动完成
	// ---------------------------
	/**
	 * 自动完成参数枚举
	 *
	 * <p>定义可以自动完成的参数类型
	 */
	public enum CompleteArgument {
		/** 名称参数 */
		@JsonProperty("name") NAME,
		/** 描述参数 */
		@JsonProperty("description") DESCRIPTION,
		/** URI参数 */
		@JsonProperty("uri") URI,
		/** MIME类型参数 */
		@JsonProperty("mimeType") MIME_TYPE
	}

	/**
	 * 自动完成请求
	 *
	 * <p>客户端请求自动完成建议
	 */
	public static class CompleteRequest implements Request {
		/**
		 * 提示词或资源引用基类
		 */
		public static class PromptOrResourceReference {
			/** 引用类型 */
			private final String type;

			/**
			 * 创建提示词或资源引用
			 *
			 * @param type 引用类型
			 */
			public PromptOrResourceReference(
					@JsonProperty("type") String type) {
				this.type = type;
			}

			public String getType() {
				return type;
			}
		}

		/**
		 * 提示词引用
		 */
		public static class PromptReference extends PromptOrResourceReference {
			/** 提示词名称 */
			private final String name;

			/**
			 * 创建提示词引用
			 *
			 * @param type 引用类型
			 * @param name 提示词名称
			 */
			public PromptReference(
					@JsonProperty("type") String type,
					@JsonProperty("name") String name) {
				super(type);
				this.name = name;
			}

			public String getName() {
				return name;
			}
		}

		/**
		 * 资源引用
		 */
		public static class ResourceReference extends PromptOrResourceReference {
			/** 资源URI */
			private final String uri;

			/**
			 * 创建资源引用
			 *
			 * @param type 引用类型
			 * @param uri 资源URI
			 */
			public ResourceReference(
					@JsonProperty("type") String type,
					@JsonProperty("uri") String uri) {
				super(type);
				this.uri = uri;
			}

			public String getUri() {
				return uri;
			}
		}

		/** 引用对象 */
		private final PromptOrResourceReference ref;
		/** 要完成的参数类型 */
		private final CompleteArgument argument;

		/**
		 * 创建自动完成请求
		 *
		 * @param ref 引用对象
		 * @param argument 要完成的参数类型
		 */
		public CompleteRequest(
				@JsonProperty("ref") PromptOrResourceReference ref,
				@JsonProperty("argument") CompleteArgument argument) {
			this.ref = ref;
			this.argument = argument;
		}

		public PromptOrResourceReference getRef() {
			return ref;
		}

		public CompleteArgument getArgument() {
			return argument;
		}
	}

	/**
	 * 自动完成结果
	 *
	 * <p>自动完成请求的响应结果
	 */
	public static class CompleteResult implements Result {
		/** 完成建议 */
		private final CompleteCompletion completion;

		/**
		 * 创建自动完成结果
		 *
		 * @param completion 完成建议
		 */
		public CompleteResult(
				@JsonProperty("completion") CompleteCompletion completion) {
			this.completion = completion;
		}

		public CompleteCompletion getCompletion() {
			return completion;
		}
	}

	/**
	 * 完成建议
	 *
	 * <p>包含自动完成的建议值
	 */
	public static class CompleteCompletion {
		/** 建议值列表 */
		private final List<String> values;
		/** 总数 */
		private final Integer total;
		/** 是否有更多结果 */
		private final Boolean hasMore;

		/**
		 * 创建完成建议
		 *
		 * @param values 建议值列表
		 * @param total 总数
		 * @param hasMore 是否有更多
		 */
		public CompleteCompletion(
				@JsonProperty("values") List<String> values,
				@JsonProperty("total") Integer total,
				@JsonProperty("hasMore") Boolean hasMore) {
			this.values = values;
			this.total = total;
			this.hasMore = hasMore;
		}

		public List<String> getValues() {
			return values;
		}

		public Integer getTotal() {
			return total;
		}

		public Boolean getHasMore() {
			return hasMore;
		}
	}

	// ---------------------------
	// 内容类型定义
	// ---------------------------
	/**
	 * 内容接口
	 *
	 * <p>定义MCP协议中使用的内容类型基类
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
			@JsonSubTypes.Type(value = ImageContent.class, name = "image"),
			@JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
	public interface Content {

		/**
		 * 获取内容类型字符串
		 *
		 * @return 内容类型字符串（"text"、"image"或"resource"）
		 */
		default String type() {
			// 根据实际类型返回对应的类型字符串
			if (this instanceof TextContent) {
				return "text";
			}
			else if (this instanceof ImageContent) {
				return "image";
			}
			else if (this instanceof EmbeddedResource) {
				return "resource";
			}
			// 未知类型，抛出异常
			throw new IllegalArgumentException("Unknown content type: " + this);
		}

	}

	/**
	 * 文本内容类
	 *
	 * <p>表示文本形式的内容
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextContent implements Content {
		/** 目标受众 */
		private final List<Role> audience;
		/** 优先级 */
		private final Double priority;
		/** 文本内容 */
		private final String text;

		/**
		 * 创建文本内容
		 *
		 * @param audience 目标受众
		 * @param priority 优先级
		 * @param text 文本内容
		 */
		public TextContent(
				@JsonProperty("audience") List<Role> audience,
				@JsonProperty("priority") Double priority,
				@JsonProperty("text") String text) {
			this.audience = audience;
			this.priority = priority;
			this.text = text;
		}

		/**
		 * 创建文本内容（简化版本）
		 *
		 * @param content 文本内容
		 */
		public TextContent(String content) {
			this(null, null, content);
		}

		public List<Role> getAudience() {
			return audience;
		}

		public Double getPriority() {
			return priority;
		}

		public String getText() {
			return text;
		}
	}

	/**
	 * 图像内容类
	 *
	 * <p>表示图像形式的内容
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ImageContent implements Content {
		/** 目标受众 */
		private final List<Role> audience;
		/** 优先级 */
		private final Double priority;
		/** 图像数据（Base64编码） */
		private final String data;
		/** MIME类型 */
		private final String mimeType;

		/**
		 * 创建图像内容
		 *
		 * @param audience 目标受众
		 * @param priority 优先级
		 * @param data 图像数据
		 * @param mimeType MIME类型
		 */
		public ImageContent(
				@JsonProperty("audience") List<Role> audience,
				@JsonProperty("priority") Double priority,
				@JsonProperty("data") String data,
				@JsonProperty("mimeType") String mimeType) {
			this.audience = audience;
			this.priority = priority;
			this.data = data;
			this.mimeType = mimeType;
		}

		@Override
		public String type() {
			return "image";
		}

		public List<Role> getAudience() {
			return audience;
		}

		public Double getPriority() {
			return priority;
		}

		public String getData() {
			return data;
		}

		public String getMimeType() {
			return mimeType;
		}
	}

	/**
	 * 嵌入资源类
	 *
	 * <p>表示嵌入的资源内容
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EmbeddedResource implements Content {
		/** 目标受众 */
		private final List<Role> audience;
		/** 优先级 */
		private final Double priority;
		/** 资源内容 */
		private final ResourceContents resource;

		/**
		 * 创建嵌入资源
		 *
		 * @param audience 目标受众
		 * @param priority 优先级
		 * @param resource 资源内容
		 */
		public EmbeddedResource(
				@JsonProperty("audience") List<Role> audience,
				@JsonProperty("priority") Double priority,
				@JsonProperty("resource") ResourceContents resource) {
			this.audience = audience;
			this.priority = priority;
			this.resource = resource;
		}

		@Override
		public String type() {
			return "resource";
		}

		public List<Role> getAudience() {
			return audience;
		}

		public Double getPriority() {
			return priority;
		}

		public ResourceContents getResource() {
			return resource;
		}
	}

	// ---------------------------
	// 根目录(Roots)
	// ---------------------------

	/**
	 * 根目录类
	 *
	 * <p>表示服务器可以访问的根目录
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Root {
		/** 根目录URI */
		private final String uri;
		/** 根目录名称 */
		private final String name;

		/**
		 * 创建根目录对象
		 *
		 * @param uri 根目录URI
		 * @param name 根目录名称
		 */
		public Root(
				@JsonProperty("uri") String uri,
				@JsonProperty("name") String name) {
			this.uri = uri;
			this.name = name;
		}

		public String getUri() {
			return uri;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * 列出根目录结果
	 *
	 * <p>服务器响应根目录列表请求的返回结果
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListRootsResult implements Result {
		/** 根目录列表 */
		private final List<Root> roots;

		/**
		 * 创建列出根目录结果
		 *
		 * @param roots 根目录列表
		 */
		public ListRootsResult(
				@JsonProperty("roots") List<Root> roots) {
			this.roots = roots;
		}

		public List<Root> getRoots() {
			return roots;
		}
	}

}
