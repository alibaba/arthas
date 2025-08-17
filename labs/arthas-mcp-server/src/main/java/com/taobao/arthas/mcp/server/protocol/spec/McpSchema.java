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
 * Based on the <a href="http://www.jsonrpc.org/specification">JSON-RPC 2.0 specification</a>
 * and the <a href="https://github.com/modelcontextprotocol/specification/blob/main/schema/2024-11-05/schema.ts">
 *     Model Context Protocol Schema</a>.
 *
 * @author Yeaury
 */
public final class McpSchema {

	private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

	private McpSchema() {
	}

	public static final String LATEST_PROTOCOL_VERSION = "2025-03-26";

	public static final String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// Method Names
	// ---------------------------

	// Lifecycle Methods
	public static final String METHOD_INITIALIZE = "initialize";

	public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	public static final String METHOD_PING = "ping";

	public static final String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";

	// Tool Methods
	public static final String METHOD_TOOLS_LIST = "tools/list";

	public static final String METHOD_TOOLS_CALL = "tools/call";

	public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	// Resources Methods
	public static final String METHOD_RESOURCES_LIST = "resources/list";

	public static final String METHOD_RESOURCES_READ = "resources/read";

	public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	// Prompt Methods
	public static final String METHOD_PROMPT_LIST = "prompts/list";

	public static final String METHOD_PROMPT_GET = "prompts/get";

	public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	// Logging Methods
	public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// Roots Methods
	public static final String METHOD_ROOTS_LIST = "roots/list";

	public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// Sampling Methods
	public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// ---------------------------
	// JSON-RPC Error Codes
	// ---------------------------
	/**
	 * Standard error codes used in MCP JSON-RPC responses.
	 */
	public static final class ErrorCodes {

		/**
		 * The JSON received by the server is invalid.
		 */
		public static final int PARSE_ERROR = -32700;

		/**
		 * The JSON sent is not a valid Request object.
		 */
		public static final int INVALID_REQUEST = -32600;

		/**
		 * The method does not exist or is unavailable.
		 */
		public static final int METHOD_NOT_FOUND = -32601;

		/**
		 * Invalid method parameters.
		 */
		public static final int INVALID_PARAMS = -32602;

		/**
		 * Internal JSON-RPC error.
		 */
		public static final int INTERNAL_ERROR = -32603;

	}

	public interface Request {
	}

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	/**
	 * Deserializes a JSON string into a JSONRPCMessage object.
	 * @param objectMapper The ObjectMapper instance to use for deserialization
	 * @param jsonText The JSON string to deserialize
	 * @return A JSONRPCMessage instance using either the {@link JSONRPCRequest},
	 * {@link JSONRPCNotification}, or {@link JSONRPCResponse} classes.
	 * @throws IOException If there's an error during deserialization
	 * @throws IllegalArgumentException If the JSON structure doesn't match any known
	 * message type
	 */
	public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
			throws IOException {

		logger.debug("Received JSON message: {}", jsonText);

		Map<String, Object> map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

		// Determine message type based on specific JSON structure
		if (map.containsKey("method") && map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCRequest.class);
		}
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCNotification.class);
		}
		else if (map.containsKey("result") || map.containsKey("error")) {
			return objectMapper.convertValue(map, JSONRPCResponse.class);
		}

		throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
	}

	// ---------------------------
	// JSON-RPC Message Types
	// ---------------------------

	/**
	 * Represents an event message with associated event ID.
	 */
	public static class EventMessage {
		private final String eventId;
		private final JSONRPCMessage message;

		public EventMessage(
				@JsonProperty("eventId") String eventId,
				@JsonProperty("message") JSONRPCMessage message) {
			this.eventId = eventId;
			this.message = message;
		}

		public String getEventId() {
			return eventId;
		}

		public JSONRPCMessage getMessage() {
			return message;
		}
	}

	public interface JSONRPCMessage {
		String getJsonrpc();
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCRequest implements JSONRPCMessage {
		private final String jsonrpc;
		private final String method;
		private final Object id;
		private final Object params;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCNotification implements JSONRPCMessage {
		private final String jsonrpc;
		private final String method;
		private final Object params;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCResponse implements JSONRPCMessage {
		private final String jsonrpc;
		private final Object id;
		private final Object result;
		private final JSONRPCError error;

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

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class JSONRPCError {
			private final int code;
			private final String message;
			private final Object data;

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
	// Initialization
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeRequest implements Request {
		private final String protocolVersion;
		private final ClientCapabilities capabilities;
		private final Implementation clientInfo;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeResult {
		private final String protocolVersion;
		private final ServerCapabilities capabilities;
		private final Implementation serverInfo;
		private final String instructions;

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
	 * Clients can implement additional features to enrich connected MCP servers with
	 * additional capabilities. These capabilities can be used to extend the functionality
	 * of the server, or to provide additional information to the server about the
	 * client's capabilities.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ClientCapabilities {

		private final Map<String, Object> experimental;
		private final RootCapabilities roots;
		private final Sampling sampling;

		public ClientCapabilities(
				@JsonProperty("experimental") Map<String, Object> experimental,
				@JsonProperty("roots") RootCapabilities roots,
				@JsonProperty("sampling") Sampling sampling) {
			this.experimental = experimental;
			this.roots = roots;
			this.sampling = sampling;
		}

		/**
		 * Roots define the boundaries of where servers can operate within the filesystem,
		 * allowing them to understand which directories and files they have access to.
		 * Servers can request the list of roots from supporting clients and
		 * receive notifications when that list changes.
         */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)	
		public static class RootCapabilities {
			private final Boolean listChanged;

			public RootCapabilities(
					@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public Boolean getListChanged() {
				return listChanged;
			}
		}

		/**
		 * Provides a standardized way for servers to request LLM
	 	 * sampling ("completions" or "generations") from language
		 * models via clients. This flow allows clients to maintain
		 * control over model access, selection, and permissions
		 * while enabling servers to leverage AI capabilities—with
		 * no server API keys necessary. Servers can request text or
		 * image-based interactions and optionally include context
		 * from MCP servers in their prompts.
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)			
		public static class Sampling {
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

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private Map<String, Object> experimental;
			private RootCapabilities roots;
			private Sampling sampling;

			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			public Builder roots(Boolean listChanged) {
				this.roots = new RootCapabilities(listChanged);
				return this;
			}

			public Builder sampling() {
				this.sampling = new Sampling();
				return this;
			}

			public ClientCapabilities build() {
				return new ClientCapabilities(experimental, roots, sampling);
			}
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ServerCapabilities {
		private final Map<String, Object> experimental;
		private final LoggingCapabilities logging;
		private final PromptCapabilities prompts;
		private final ResourceCapabilities resources;
		private final ToolCapabilities tools;

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

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private Map<String, Object> experimental;
			private LoggingCapabilities logging;
			private PromptCapabilities prompts;
			private ResourceCapabilities resources;
			private ToolCapabilities tools;

			public Builder experimental(Map<String, Object> experimental) {
				this.experimental = experimental;
				return this;
			}

			public Builder logging(LoggingCapabilities logging) {
				this.logging = logging;
				return this;
			}

			public Builder prompts(PromptCapabilities prompts) {
				this.prompts = prompts;
				return this;
			}

			public Builder resources(ResourceCapabilities resources) {
				this.resources = resources;
				return this;
			}

			public Builder tools(ToolCapabilities tools) {
				this.tools = tools;
				return this;
			}

			public ServerCapabilities build() {
				return new ServerCapabilities(experimental, logging, prompts, resources, tools);
			}
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class LoggingCapabilities {
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class PromptCapabilities {
			private final Boolean listChanged;

			public PromptCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public Boolean getListChanged() {
				return listChanged;
			}
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ResourceCapabilities {
			private final Boolean subscribe;
			private final Boolean listChanged;

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

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ToolCapabilities {
			private final Boolean listChanged;

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


	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Implementation {
		private final String name;
		private final String version;

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

	// Existing Enums and Base Types
	public enum Role {
		@JsonProperty("user") USER,
		@JsonProperty("assistant") ASSISTANT
	}

	public enum StopReason {
		@JsonProperty("stop") STOP,
		@JsonProperty("length") LENGTH,
		@JsonProperty("content_filter") CONTENT_FILTER
	}

	public enum ContextInclusionStrategy {
		@JsonProperty("none") NONE,
		@JsonProperty("all") ALL,
		@JsonProperty("relevant") RELEVANT
	}

	// ---------------------------
	// Resource Interfaces
	// ---------------------------
	/**
	 * Base for objects that include optional annotations for the client. The client can
	 * use annotations to inform how objects are used or displayed
	 */
	public interface Annotated {

		Annotations annotations();

	}

	/**
	 * Optional annotations for the client. The client can use annotations to inform how
	 * objects are used or displayed.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Annotations {
		private final List<Role> audience;
		private final Double priority;

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
	 * A known resource that the server is capable of reading.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Resource implements Annotated {
		private final String uri;
		private final String name;
		private final String description;
		private final String mimeType;
		private final Annotations annotations;

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
	 * Resource templates allow servers to expose parameterized resources using URI
	 * templates.
	 *
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ResourceTemplate implements Annotated {
		private final String uriTemplate;
		private final String name;
		private final String description;
		private final String mimeType;
		private final Annotations annotations;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourcesResult {
		private final List<Resource> resources;
		private final String nextCursor;

		public ListResourcesResult(
				@JsonProperty("resources") List<Resource> resources,
				@JsonProperty("nextCursor") String nextCursor) {
			this.resources = resources;
			this.nextCursor = nextCursor;
		}

		public List<Resource> getResources() {
			return resources;
		}

		public String getNextCursor() {
			return nextCursor;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourceTemplatesResult {
		private final List<ResourceTemplate> resourceTemplates;
		private final String nextCursor;

		public ListResourceTemplatesResult(
				@JsonProperty("resourceTemplates") List<ResourceTemplate> resourceTemplates,
				@JsonProperty("nextCursor") String nextCursor) {
			this.resourceTemplates = resourceTemplates;
			this.nextCursor = nextCursor;
		}

		public List<ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		public String getNextCursor() {
			return nextCursor;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceRequest {
		private final String uri;

		public ReadResourceRequest(
				@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceResult {
		private final List<ResourceContents> contents;

		public ReadResourceResult(
				@JsonProperty("contents") List<ResourceContents> contents) {
			this.contents = contents;
		}

		public List<ResourceContents> getContents() {
			return contents;
		}
	}

	/**
	 * Sent from the client to request resources/updated notifications from the server
	 * whenever a particular resource changes.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SubscribeRequest {
		private final String uri;

		public SubscribeRequest(
				@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UnsubscribeRequest {
		private final String uri;

		public UnsubscribeRequest(
				@JsonProperty("uri") String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	/**
	 * The contents of a specific resource or sub-resource.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
			@JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
	public interface ResourceContents {

		/**
		 * The URI of this resource.
		 * @return the URI of this resource.
		 */
		String uri();

		/**
		 * The MIME type of this resource.
		 * @return the MIME type of this resource.
		 */
		String mimeType();

	}

	/**
	 * Text contents of a resource.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextResourceContents implements ResourceContents {
		private final String uri;
		private final String mimeType;
		private final String text;

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
	 * Binary contents of a resource.
	 *
	 * This must only be set if the resource can actually be represented as binary data
	 * (not text).
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BlobResourceContents implements ResourceContents {
		private final String uri;
		private final String mimeType;
		private final String blob;

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
	// Prompt Interfaces
	// ---------------------------
	/**
	 * A prompt or prompt template that the server offers.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Prompt {
		private final String name;
		private final String description;
		private final List<PromptArgument> arguments;

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
	 * Describes an argument that a prompt can accept.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptArgument {
		private final String name;
		private final String description;
		private final Boolean required;

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
	 * Describes a message returned as part of a prompt.
	 *
	 * This is similar to `SamplingMessage`, but also supports the embedding of resources
	 * from the MCP server.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptMessage {
		private final Role role;
		private final Content content;

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
	 * The server's response to a prompts/list request from the client.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListPromptsResult {
		private final List<Prompt> prompts;
		private final String nextCursor;

		public ListPromptsResult(
				@JsonProperty("prompts") List<Prompt> prompts,
				@JsonProperty("nextCursor") String nextCursor) {
			this.prompts = prompts;
			this.nextCursor = nextCursor;
		}

		public List<Prompt> getPrompts() {
			return prompts;
		}

		public String getNextCursor() {
			return nextCursor;
		}
	}

	/**
	 * Used by the client to get a prompt provided by the server.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptRequest implements Request {
		private final String name;
		private final Map<String, Object> arguments;

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
	 * The server's response to a prompts/get request from the client.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptResult {
		private final String description;
		private final List<PromptMessage> messages;

		public GetPromptResult(
				@JsonProperty("description") String description,
				@JsonProperty("messages") List<PromptMessage> messages) {
			this.description = description;
			this.messages = messages;
		}

		public String getDescription() {
			return description;
		}

		public List<PromptMessage> getMessages() {
			return messages;
		}
	}

	// ---------------------------
	// Tool Interfaces
	// ---------------------------
	/**
	 * The server's response to a tools/list request from the client.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListToolsResult {
		private final List<Tool> tools;
		private final String nextCursor;

		public ListToolsResult(
				@JsonProperty("tools") List<Tool> tools,
				@JsonProperty("nextCursor") String nextCursor) {
			this.tools = tools;
			this.nextCursor = nextCursor;
		}

		public List<Tool> getTools() {
			return tools;
		}

		public String getNextCursor() {
			return nextCursor;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JsonSchema {
		private final String type;
		private final Map<String, Object> properties;
		private final List<String> required;
		private final Boolean additionalProperties;

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
	 * Represents a tool that the server provides. Tools enable servers to expose
	 * executable functionality to the system. Through these tools, you can interact with
	 * external systems, perform computations, and take actions in the real world.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Tool {
		private final String name;
		private final String description;
		private final JsonSchema inputSchema;

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

	private static JsonSchema parseSchema(String schema) {
		try {
			return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Invalid schema: " + schema, e);
		}
	}

	/**
	 * Used by the client to call a tool provided by the server.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolRequest implements Request {
		private final String name;
		private final Map<String, Object> arguments;
		private final Map<String, Object> meta;


		public CallToolRequest(
				@JsonProperty("name") String name,
				@JsonProperty("arguments") Map<String, Object> arguments,
				@JsonProperty("_meta") Map<String, Object> meta) {
			this.name = name;
			this.arguments = arguments;
			this.meta = meta;
		}

		public CallToolRequest(String name, String jsonArguments) {
			this(name, parseJsonArguments(jsonArguments), null);
		}

		public CallToolRequest(String name, Map<String, Object> arguments) {
			this(name, arguments, null);
		}

		private static Map<String, Object> parseJsonArguments(String jsonArguments) {
			try {
				return OBJECT_MAPPER.readValue(jsonArguments, MAP_TYPE_REF);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
			}
		}

		public String getName() {
			return name;
		}

		public Map<String, Object> getArguments() {
			return arguments;
		}

		public Map<String, Object> getMeta() {
			return meta;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private String name;

			private Map<String, Object> arguments;

			private Map<String, Object> meta;

			public Builder name(String name) {
				this.name = name;
				return this;
			}

			public Builder arguments(Map<String, Object> arguments) {
				this.arguments = arguments;
				return this;
			}

			public Builder arguments(String jsonArguments) {
				this.arguments = parseJsonArguments(jsonArguments);
				return this;
			}

			public Builder meta(Map<String, Object> meta) {
				this.meta = meta;
				return this;
			}

			public Builder progressToken(String progressToken) {
				if (this.meta == null) {
					this.meta = new HashMap<>();
				}
				this.meta.put("progressToken", progressToken);
				return this;
			}

			public CallToolRequest build() {
				Assert.hasText(name, "name must not be empty");
				return new CallToolRequest(name, arguments, meta);
			}
		}
	}

	/**
	 * The server's response to a tools/call request from the client.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolResult {
		private final List<Content> content;
		private final Boolean isError;

		public CallToolResult(
				@JsonProperty("content") List<Content> content,
				@JsonProperty("isError") Boolean isError) {
			this.content = content;
			this.isError = isError;
		}

		public CallToolResult(String content, Boolean isError) {
			this(Collections.singletonList(new TextContent(content)), isError);
		}

		public List<Content> getContent() {
			return content;
		}

		public Boolean getIsError() {
			return isError;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private List<Content> content = new ArrayList<>();
			private Boolean isError;

			public Builder content(List<Content> content) {
				Assert.notNull(content, "content must not be null");
				this.content = content;
				return this;
			}

			public Builder textContent(List<String> textContent) {
				Assert.notNull(textContent, "textContent must not be null");
				textContent.stream()
						.map(TextContent::new)
						.forEach(this.content::add);
				return this;
			}

			public Builder addContent(Content contentItem) {
				Assert.notNull(contentItem, "contentItem must not be null");
				if (this.content == null) {
					this.content = new ArrayList<>();
				}
				this.content.add(contentItem);
				return this;
			}

			public Builder addTextContent(String text) {
				Assert.notNull(text, "text must not be null");
				return addContent(new TextContent(text));
			}

			public Builder isError(Boolean isError) {
				Assert.notNull(isError, "isError must not be null");
				this.isError = isError;
				return this;
			}

			public CallToolResult build() {
				return new CallToolResult(content, isError);
			}
		}
	}

	// ---------------------------
	// Sampling Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelPreferences {
		private final List<ModelHint> hints;
		private final Double costPriority;
		private final Double speedPriority;
		private final Double intelligencePriority;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelHint {
		private final String name;

		public ModelHint(
				@JsonProperty("name") String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SamplingMessage {
		private final Role role;
		private final Content content;

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

	// Sampling and Message Creation
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageRequest implements Request {
		private final List<SamplingMessage> messages;
		private final ModelPreferences modelPreferences;
		private final String systemPrompt;
		private final ContextInclusionStrategy includeContext;
		private final Double temperature;
		private final int maxTokens;
		private final List<String> stopSequences;
		private final Map<String, Object> metadata;

		public CreateMessageRequest(
				@JsonProperty("messages") List<SamplingMessage> messages,
				@JsonProperty("modelPreferences") ModelPreferences modelPreferences,
				@JsonProperty("systemPrompt") String systemPrompt,
				@JsonProperty("includeContext") ContextInclusionStrategy includeContext,
				@JsonProperty("temperature") Double temperature,
				@JsonProperty("maxTokens") int maxTokens,
				@JsonProperty("stopSequences") List<String> stopSequences,
				@JsonProperty("metadata") Map<String, Object> metadata) {
			this.messages = messages;
			this.modelPreferences = modelPreferences;
			this.systemPrompt = systemPrompt;
			this.includeContext = includeContext;
			this.temperature = temperature;
			this.maxTokens = maxTokens;
			this.stopSequences = stopSequences;
			this.metadata = metadata;
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

		public Map<String, Object> getMetadata() {
			return metadata;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageResult {
		private final Role role;
		private final Content content;
		private final String model;
		private final StopReason stopReason;

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

	// ---------------------------
	// Pagination Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedRequest {
		private final String cursor;

		public PaginatedRequest(
				@JsonProperty("cursor") String cursor) {
			this.cursor = cursor;
		}

		public String getCursor() {
			return cursor;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedResult {
		private final String nextCursor;

		public PaginatedResult(
				@JsonProperty("nextCursor") String nextCursor) {
			this.nextCursor = nextCursor;
		}

		public String getNextCursor() {
			return nextCursor;
		}
	}

	// ---------------------------
	// Progress and Logging
	// ---------------------------
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProgressNotification {
		private final int progressToken;
		private final double progress;
		private final Double total;

		public ProgressNotification(
				@JsonProperty("progressToken") int progressToken,
				@JsonProperty("progress") double progress,
				@JsonProperty("total") Double total) {
			this.progressToken = progressToken;
			this.progress = progress;
			this.total = total;
		}

		public int getProgressToken() {
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
	 * The Model Context Protocol (MCP) provides a standardized way for servers to send
	 * structured log messages to clients. Clients can control logging verbosity by
	 * setting minimum log levels, with servers sending notifications containing severity
	 * levels, optional logger names, and arbitrary JSON-serializable data.
	 *
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LoggingMessageNotification {
		private final LoggingLevel level;
		private final String logger;
		private final Object data;

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

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private LoggingLevel level = LoggingLevel.INFO;
			private String logger = "server";
			private Object data;

			public Builder level(LoggingLevel level) {
				this.level = level;
				return this;
			}

			public Builder logger(String logger) {
				this.logger = logger;
				return this;
			}

			public Builder data(Object data) {
				this.data = data;
				return this;
			}

			public LoggingMessageNotification build() {
				return new LoggingMessageNotification(level, logger, data);
			}
		}
	}

	public enum LoggingLevel {
		@JsonProperty("debug") DEBUG(0),
		@JsonProperty("info") INFO(1),
		@JsonProperty("notice") NOTICE(2),
		@JsonProperty("warning") WARNING(3),
		@JsonProperty("error") ERROR(4),
		@JsonProperty("critical") CRITICAL(5),
		@JsonProperty("alert") ALERT(6),
		@JsonProperty("emergency") EMERGENCY(7);

		private final int level;

		LoggingLevel(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SetLevelRequest {
		private final LoggingLevel level;

		public SetLevelRequest(
				@JsonProperty("level") LoggingLevel level) {
			this.level = level;
		}

		public LoggingLevel getLevel() {
			return level;
		}
	}

	/**
	 * Notification for sending intermediate results during streaming tool execution.
	 * This allows tools to send partial results to clients in real-time.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class IntermediateResultNotification {
		private final String type;
		private final Object data;

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
	// Autocomplete
	// ---------------------------
	public enum CompleteArgument {
		@JsonProperty("name") NAME,
		@JsonProperty("description") DESCRIPTION,
		@JsonProperty("uri") URI,
		@JsonProperty("mimeType") MIME_TYPE
	}

	public static class CompleteRequest implements Request {
		public static class PromptOrResourceReference {
			private final String type;

			public PromptOrResourceReference(
					@JsonProperty("type") String type) {
				this.type = type;
			}

			public String getType() {
				return type;
			}
		}

		public static class PromptReference extends PromptOrResourceReference {
			private final String name;

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

		public static class ResourceReference extends PromptOrResourceReference {
			private final String uri;

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

		private final PromptOrResourceReference ref;
		private final CompleteArgument argument;

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

	public static class CompleteResult {
		private final CompleteCompletion completion;

		public CompleteResult(
				@JsonProperty("completion") CompleteCompletion completion) {
			this.completion = completion;
		}

		public CompleteCompletion getCompletion() {
			return completion;
		}
	}

	public static class CompleteCompletion {
		private final List<String> values;
		private final Integer total;
		private final Boolean hasMore;

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
	// Content Types
	// ---------------------------
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
			@JsonSubTypes.Type(value = ImageContent.class, name = "image"),
			@JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
	public interface Content {

		default String type() {
			if (this instanceof TextContent) {
				return "text";
			}
			else if (this instanceof ImageContent) {
				return "image";
			}
			else if (this instanceof EmbeddedResource) {
				return "resource";
			}
			throw new IllegalArgumentException("Unknown content type: " + this);
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextContent implements Content {
		private final List<Role> audience;
		private final Double priority;
		private final String text;

		public TextContent(
				@JsonProperty("audience") List<Role> audience,
				@JsonProperty("priority") Double priority,
				@JsonProperty("text") String text) {
			this.audience = audience;
			this.priority = priority;
			this.text = text;
		}

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ImageContent implements Content {
		private final List<Role> audience;
		private final Double priority;
		private final String data;
		private final String mimeType;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EmbeddedResource implements Content {
		private final List<Role> audience;
		private final Double priority;
		private final ResourceContents resource;

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
	// Roots
	// ---------------------------

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Root {
		private final String uri;
		private final String name;

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

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListRootsResult {
		private final List<Root> roots;

		public ListRootsResult(
				@JsonProperty("roots") List<Root> roots) {
			this.roots = roots;
		}

		public List<Root> getRoots() {
			return roots;
		}
	}

}
