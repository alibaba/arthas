package com.taobao.arthas.core.mcp.util;

import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP专用的ObjectVO序列化过滤器
 * <p>
 * 该类实现了FastJSON2的ValueFilter接口，用于在JSON序列化过程中
 * 特殊处理ObjectVO对象。ObjectVO是Arthas中用于表示Java对象的包装类，
 * 该过滤器根据配置决定如何将ObjectVO转换为字符串表示。
 * </p>
 * <p>
 * 主要功能：</p>
 * <ul>
 * <li>拦截ObjectVO对象的序列化过程</li>
 * <li>根据GlobalOptions.isUsingJson配置选择JSON格式或文本格式输出</li>
 * <li>处理对象展开(expand)逻辑</li>
 * <li>处理序列化异常，提供降级方案</li>
 * </ul>
 */
public class McpObjectVOFilter implements ValueFilter {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(McpObjectVOFilter.class);

    /**
     * 单例实例
     * <p>采用单例模式，确保全局只有一个过滤器实例</p>
     */
    private static final McpObjectVOFilter INSTANCE = new McpObjectVOFilter();

    /**
     * 注册标志
     * <p>使用volatile确保多线程环境下的可见性</p>
     * <p>true表示已注册到JsonParser，false表示未注册</p>
     */
    private static volatile boolean registered = false;
    
    /**
     * 将此过滤器注册到JsonParser
     * <p>
     * 使用双重检查锁定(DCL)模式确保线程安全的单例注册。
     * 注册后，JsonParser在序列化时会使用此过滤器处理ObjectVO对象。
     * </p>
     */
    public static void register() {
        // 第一次检查：避免不必要的同步
        if (!registered) {
            // 同步代码块：确保只有一个线程能执行注册
            synchronized (McpObjectVOFilter.class) {
                // 第二次检查：防止多个线程通过第一次检查后重复注册
                if (!registered) {
                    // 注册过滤器实例到JsonParser
                    JsonParser.registerFilter(INSTANCE);
                    // 标记为已注册
                    registered = true;
                    // 记录调试信息
                    logger.debug("McpObjectVOFilter registered to JsonParser");
                }
            }
        }
    }
    
    /**
     * 应用过滤器处理值
     * <p>
     * FastJSON2在序列化时会调用此方法，允许我们修改或替换要序列化的值。
     * 该方法检查值是否为ObjectVO类型，如果是则进行特殊处理。
     * </p>
     *
     * @param object 包含该属性的对象
     * @param name 属性名称
     * @param value 属性值
     * @return 处理后的值，如果是ObjectVO则返回其字符串表示，否则返回原值
     */
    @Override
    public Object apply(Object object, String name, Object value) {
        // 如果值为null，直接返回null
        if (value == null) {
            return null;
        }

        // 使用直接类型检查而非反射，性能更好
        if (value instanceof ObjectVO) {
            // 处理ObjectVO对象，转换为字符串表示
            return handleObjectVO((ObjectVO) value);
        }

        // 非ObjectVO对象，直接返回原值
        return value;
    }

    /**
     * 处理ObjectVO对象
     * <p>
     * 该方法根据ObjectVO的配置和全局设置，将ObjectVO转换为适当的字符串表示。
     * 处理逻辑包括：
     * <ul>
     * <li>检查内部对象是否为null</li>
     * <li>判断是否需要展开(expand)对象</li>
     * <li>根据全局配置选择JSON或文本格式</li>
     * <li>处理异常情况</li>
     * </ul>
     * </p>
     *
     * @param objectVO 要处理的ObjectVO对象
     * @return 对象的字符串表示
     */
    private Object handleObjectVO(ObjectVO objectVO) {
        try {
            // 获取ObjectVO包装的内部对象
            Object innerObject = objectVO.getObject();
            // 获取展开级别配置
            Integer expand = objectVO.getExpand();

            // 如果内部对象为null，返回字符串"null"
            if (innerObject == null) {
                return "null";
            }

            // 判断是否需要展开对象
            if (objectVO.needExpand()) {
                // 根据 GlobalOptions.isUsingJson 配置决定输出格式
                if (GlobalOptions.isUsingJson) {
                    // 使用JSON格式输出
                    return drawJsonView(innerObject);
                } else {
                    // 使用ObjectView输出对象结构
                    return drawObjectView(objectVO);
                }
            } else {
                // 不需要展开，直接转换为字符串
                return objectToString(innerObject);
            }
        } catch (Exception e) {
            // 记录警告日志
            logger.warn("Failed to handle ObjectVO: {}", e.getMessage());
            // 返回错误信息的JSON格式
            return "{\"error\":\"ObjectVO serialization failed\"}";
        }
    }

    /**
     * 使用 ObjectView 输出对象结构
     * <p>
     * 创建ObjectView实例并调用其draw()方法生成对象的文本表示。
     * ObjectView会以树形结构展示对象的层次和属性。
     * </p>
     *
     * @param objectVO 包含对象信息的ObjectVO
     * @return 对象的文本表示
     */
    private String drawObjectView(ObjectVO objectVO) {
        try {
            // 创建ObjectView实例，传入ObjectVO配置
            ObjectView objectView = new ObjectView(objectVO);
            // 调用draw方法生成对象结构的文本表示
            return objectView.draw();
        } catch (Exception e) {
            // 记录调试信息：ObjectView序列化失败
            logger.debug("ObjectView serialization failed, using toString: {}", e.getMessage());
            // 降级处理：使用toString方法
            return objectToString(objectVO.getObject());
        }
    }

    /**
     * 使用 JSON 格式输出对象
     * <p>
     * 调用ObjectView的toJsonString方法将对象转换为JSON格式的字符串。
     * 这种格式适合需要结构化数据的场景。
     * </p>
     *
     * @param object 要转换的对象
     * @return 对象的JSON字符串表示
     */
    private String drawJsonView(Object object) {
        try {
            // 使用ObjectView的工具方法转换为JSON字符串
            return ObjectView.toJsonString(object);
        } catch (Exception e) {
            // 记录调试信息：JSON序列化失败
            logger.debug("ObjectView-style serialization failed, using toString: {}", e.getMessage());
            // 降级处理：使用toString方法
            return objectToString(object);
        }
    }

    /**
     * 将对象转换为字符串
     * <p>
     * 这是一个降级方法，当其他序列化方法失败时使用。
     * 尝试调用对象的toString()方法，如果失败则返回对象的类名和哈希码。
     * </p>
     *
     * @param object 要转换的对象
     * @return 对象的字符串表示
     */
    private String objectToString(Object object) {
        // 如果对象为null，返回字符串"null"
        if (object == null) {
            return "null";
        }
        try {
            // 尝试调用对象的toString()方法
            return object.toString();
        } catch (Exception e) {
            // 如果toString()失败，返回类名@哈希码的格式
            // 这是Java对象的默认toString格式
            return object.getClass().getSimpleName() + "@" + Integer.toHexString(object.hashCode());
        }
    }
}
