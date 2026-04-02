package com.taobao.arthas.core.view;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriterCreator;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ObjectVO;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;

/**
 * 对象视图控件<br/>
 * 能够展示对象的内部结构，支持多种数据类型的格式化显示
 * 支持的类型包括：基本类型、字符串、集合、Map、数组、Throwable、Date、枚举和普通对象
 * Created by vlinux on 15/5/20.
 */
public class ObjectView implements View {
    // 对象展示的最大深度限制，防止无限递归
    public static final int MAX_DEEP = 4;
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ObjectView.class);
    // fastjson2的对象写入器提供者，用于自定义JSON序列化行为
    private static final ObjectWriterProvider JSON_OBJECT_WRITER_PROVIDER = new ObjectWriterProvider(
            new ObjectWriterCreator() {
                @Override
                protected void setDefaultValue(List<FieldWriter> fieldWriters, Class objectClass) {
                    // fastjson2 默认会通过无参构造函数创建一个对象来提取字段默认值（用于 NotWriteDefaultValue 等能力），
                    // 这可能触发目标对象的构造逻辑（比如单例守卫、资源初始化等），在 Arthas 里属于不可接受的副作用。
                    // 这里直接禁用该行为，只基于现有对象进行序列化。
                }
            });

    /**
     * 将对象转换为JSON字符串
     * 使用自定义的ObjectWriterProvider避免触发目标对象的构造函数
     * @param object 要转换的对象
     * @return JSON格式的字符串
     */
    public static String toJsonString(Object object) {
        // 创建JSON写入上下文，使用自定义的ObjectWriterProvider
        JSONWriter.Context context = new JSONWriter.Context(JSON_OBJECT_WRITER_PROVIDER);
        // 设置最大深度为4097，防止循环引用导致的栈溢出
        context.setMaxLevel(4097);
        // 配置JSON序列化特性：
        // - IgnoreErrorGetter: 忽略getter方法访问错误
        // - ReferenceDetection: 启用引用检测，避免循环引用
        // - IgnoreNonFieldGetter: 忽略非字段对应的getter方法
        // - WriteNonStringKeyAsString: 将Map的非String键转换为字符串
        context.config(JSONWriter.Feature.IgnoreErrorGetter,
                JSONWriter.Feature.ReferenceDetection,
                JSONWriter.Feature.IgnoreNonFieldGetter,
                JSONWriter.Feature.WriteNonStringKeyAsString);
        return JSON.toJSONString(object, context);
    }

    // 要展示的对象
    private final Object object;
    // 当前展示的深度层级
    private final int deep;
    // 对象字符串的最大长度限制
    private final int maxObjectLength;

    /**
     * 使用ObjectVO构造ObjectView
     * @param objectVO 包含对象和展开配置的值对象
     */
    public ObjectView(ObjectVO objectVO) {
        this(defaultMaxObjectLength(), objectVO);
    }

    /**
     * 使用ObjectVO和指定的最大对象长度构造ObjectView
     * int参数在前面，防止构造函数二义性
     * @param maxObjectLength 对象字符串的最大长度限制
     * @param objectVO 包含对象和展开配置的值对象
     */
    public ObjectView(int maxObjectLength, ObjectVO objectVO) {
        this(objectVO.getObject(), objectVO.expandOrDefault(), maxObjectLength);
    }

    /**
     * 使用对象和深度构造ObjectView
     * @param object 要展示的对象
     * @param deep 展开的深度
     */
    public ObjectView(Object object, int deep) {
        this(object, deep, defaultMaxObjectLength());
    }

    /**
     * 使用对象、深度和最大长度构造ObjectView
     * @param object 要展示的对象
     * @param deep 展开的深度
     * @param maxObjectLength 对象字符串的最大长度限制
     */
    public ObjectView(Object object, int deep, int maxObjectLength) {
        this.object = object;
        // 确保深度不超过最大深度限制
        this.deep = deep > MAX_DEEP ? MAX_DEEP : deep;
        this.maxObjectLength = maxObjectLength;
    }

    /**
     * 绘制对象的视图
     * 根据全局配置决定使用JSON格式还是文本格式展示
     * @return 对象的字符串表示
     */
    @Override
    public String draw() {
        StringBuilder buf = new StringBuilder();
        try {
            // 如果配置使用JSON格式，则转换为JSON字符串
            if (GlobalOptions.isUsingJson) {
                return toJsonString(object);
            }
            // 否则使用文本格式渲染对象
            renderObject(object, 0, deep, buf);
            return buf.toString();
        } catch (ObjectTooLargeException e) {
            // 对象大小超过限制时的处理
            buf.append(" Object size exceeds size limit: ")
                    .append(maxObjectLength)
                    .append(", try to use `options object-size-limit <bytes>` to increase the limit.");
            return buf.toString();
        } catch (Throwable t) {
            // 其他异常的处理，记录错误日志并返回错误信息
            logger.error("ObjectView draw error, object class: {}", object.getClass(), t);
            return "ERROR DATA!!! object class: " + object.getClass() + ", exception class: " + t.getClass()
                    + ", exception message: " + t.getMessage();
        }
    }

    // 制表符，用于缩进展示
    private final static String TAB = "    ";

    // ASCII控制字符的映射表，用于将控制字符转换为可读的名称
    private final static Map<Byte, String> ASCII_MAP = new HashMap<Byte, String>();

    // 静态初始化块：初始化ASCII控制字符的映射表
    static {
        ASCII_MAP.put((byte) 0, "NUL");   // Null字符
        ASCII_MAP.put((byte) 1, "SOH");   // Start of Heading
        ASCII_MAP.put((byte) 2, "STX");   // Start of Text
        ASCII_MAP.put((byte) 3, "ETX");   // End of Text
        ASCII_MAP.put((byte) 4, "EOT");   // End of Transmission
        ASCII_MAP.put((byte) 5, "ENQ");   // Enquiry
        ASCII_MAP.put((byte) 6, "ACK");   // Acknowledge
        ASCII_MAP.put((byte) 7, "BEL");   // Bell（响铃）
        ASCII_MAP.put((byte) 8, "BS");    // Backspace（退格）
        ASCII_MAP.put((byte) 9, "HT");    // Horizontal Tab（水平制表符）
        ASCII_MAP.put((byte) 10, "LF");   // Line Feed（换行）
        ASCII_MAP.put((byte) 11, "VT");   // Vertical Tab（垂直制表符）
        ASCII_MAP.put((byte) 12, "FF");   // Form Feed（换页）
        ASCII_MAP.put((byte) 13, "CR");   // Carriage Return（回车）
        ASCII_MAP.put((byte) 14, "SO");   // Shift Out
        ASCII_MAP.put((byte) 15, "SI");   // Shift In
        ASCII_MAP.put((byte) 16, "DLE");  // Data Link Escape
        ASCII_MAP.put((byte) 17, "DC1");  // Device Control 1
        ASCII_MAP.put((byte) 18, "DC2");  // Device Control 2
        ASCII_MAP.put((byte) 19, "DC3");  // Device Control 3
        ASCII_MAP.put((byte) 20, "DC4");  // Device Control 4
        ASCII_MAP.put((byte) 21, "NAK");  // Negative Acknowledge
        ASCII_MAP.put((byte) 22, "SYN");  // Synchronous Idle
        ASCII_MAP.put((byte) 23, "ETB");  // End of Transmission Block
        ASCII_MAP.put((byte) 24, "CAN");  // Cancel
        ASCII_MAP.put((byte) 25, "EM");   // End of Medium
        ASCII_MAP.put((byte) 26, "SUB");  // Substitute
        ASCII_MAP.put((byte) 27, "ESC");  // Escape（转义）
        ASCII_MAP.put((byte) 28, "FS");   // File Separator
        ASCII_MAP.put((byte) 29, "GS");   // Group Separator
        ASCII_MAP.put((byte) 30, "RS");   // Record Separator
        ASCII_MAP.put((byte) 31, "US");   // Unit Separator
        ASCII_MAP.put((byte) 127, "DEL"); // Delete（删除）
    }

    /**
     * 渲染对象的内部结构
     * 根据对象的类型采用不同的渲染策略
     * @param obj 要渲染的对象
     * @param deep 当前渲染深度
     * @param expand 展开的深度限制
     * @param buf 输出缓冲区
     * @throws ObjectTooLargeException 当对象大小超过限制时抛出
     */
    private void renderObject(Object obj, int deep, int expand, final StringBuilder buf) throws ObjectTooLargeException {

        if (null == obj) {
            // 处理null值
            appendStringBuilder(buf,"null");
        } else {

            final Class<?> clazz = obj.getClass();
            final String className = clazz.getSimpleName();

            // 处理7种基础类型包装类（Integer、Long、Float、Double、Short、Byte、Boolean）
            // 直接输出格式为 @类型[值]
            if (Integer.class.isInstance(obj)
                || Long.class.isInstance(obj)
                || Float.class.isInstance(obj)
                || Double.class.isInstance(obj)
                    //                    || Character.class.isInstance(obj)  // Character单独处理，因为有不可见字符
                || Short.class.isInstance(obj)
                || Byte.class.isInstance(obj)
                || Boolean.class.isInstance(obj)) {
                appendStringBuilder(buf, format("@%s[%s]", className, obj));
            }

            // Character类型需要特殊处理，因为有不可见字符的因素
            else if (Character.class.isInstance(obj)) {

                final Character c = (Character) obj;

                // ASCII的可见字符范围（32-126）
                if (c >= 32
                    && c <= 126) {
                    appendStringBuilder(buf, format("@%s[%s]", className, c));
                }

                // ASCII的控制字符（0-31, 127），从映射表获取可读名称
                else if (ASCII_MAP.containsKey((byte) c.charValue())) {
                    appendStringBuilder(buf, format("@%s[%s]", className, ASCII_MAP.get((byte) c.charValue())));
                }

                // 超过ASCII的编码范围（如Unicode字符）
                else {
                    appendStringBuilder(buf, format("@%s[%s]", className, c));
                }

            }

            // 字符串类型单独处理，特殊处理换行符和回车符
            else if (String.class.isInstance(obj)) {
                appendStringBuilder(buf, "@");
                appendStringBuilder(buf, className);
                appendStringBuilder(buf, "[");
                // 遍历字符串的每个字符
                for (Character c : ((String) obj).toCharArray()) {
                    switch (c) {
                        case '\n':
                            // 换行符转义为 \n
                            appendStringBuilder(buf, "\\n");
                            break;
                        case '\r':
                            // 回车符转义为 \r
                            appendStringBuilder(buf, "\\r");
                            break;
                        default:
                            // 其他字符直接输出
                            appendStringBuilder(buf, c.toString());
                    }//switch
                }//for
                appendStringBuilder(buf, "]");
            }

            // 集合类输出（List、Set等）
            else if (Collection.class.isInstance(obj)) {

                @SuppressWarnings("unchecked") final Collection<Object> collection = (Collection<Object>) obj;

                // 如果不需要展开或是空集合，只展示摘要信息
                if (!isExpand(deep, expand)
                    || collection.isEmpty()) {

                    appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                      className,
                                      collection.isEmpty(),
                                      collection.size()));
                }

                // 展开展示集合中的每个元素
                else {
                    appendStringBuilder(buf, format("@%s[", className));
                    for (Object e : collection) {
                        appendStringBuilder(buf, "\n");
                        // 根据深度添加缩进
                        for (int i = 0; i < deep+1; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        // 递归渲染每个元素
                        renderObject(e, deep + 1, expand, buf);
                        appendStringBuilder(buf, ",");
                    }
                    appendStringBuilder(buf, "\n");
                    // 闭合括号对齐
                    for (int i = 0; i < deep; i++) {
                        appendStringBuilder(buf, TAB);
                    }
                    appendStringBuilder(buf, "]");
                }

            }


            // Map类输出（HashMap、TreeMap等）
            else if (Map.class.isInstance(obj)) {
                @SuppressWarnings("unchecked") final Map<Object, Object> map = (Map<Object, Object>) obj;

                // 如果不需要展开或是空Map，只展示摘要信息
                if (!isExpand(deep, expand)
                    || map.isEmpty()) {

                    appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                      className,
                                      map.isEmpty(),
                                      map.size()));

                } else {
                    // 展开展示Map中的每个键值对
                    appendStringBuilder(buf, format("@%s[", className));
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        appendStringBuilder(buf, "\n");
                        // 根据深度添加缩进
                        for (int i = 0; i < deep+1; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        // 渲染键
                        renderObject(entry.getKey(), deep + 1, expand, buf);
                        appendStringBuilder(buf, ":");
                        // 渲染值
                        renderObject(entry.getValue(), deep + 1, expand, buf);
                        appendStringBuilder(buf, ",");
                    }
                    appendStringBuilder(buf, "\n");
                    // 闭合括号对齐
                    for (int i = 0; i < deep; i++) {
                        appendStringBuilder(buf, TAB);
                    }
                    appendStringBuilder(buf, "]");
                }
            }


            // 数组类输出，包括8种基本类型数组和对象数组
            else if (obj.getClass().isArray()) {


                final String typeName = obj.getClass().getSimpleName();

                // 处理int[]数组
                if (typeName.equals("int[]")) {

                    final int[] arrays = (int[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (int e : arrays) {
                            appendStringBuilder(buf, "\n");
                            // 根据深度添加缩进
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            // 递归渲染每个元素（int会被自动装箱为Integer）
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        // 闭合括号对齐
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理long[]数组
                else if (typeName.equals("long[]")) {

                    final long[] arrays = (long[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (long e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理short[]数组
                else if (typeName.equals("short[]")) {

                    final short[] arrays = (short[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (short e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理float[]数组
                else if (typeName.equals("float[]")) {

                    final float[] arrays = (float[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (float e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理double[]数组
                else if (typeName.equals("double[]")) {

                    final double[] arrays = (double[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (double e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理boolean[]数组
                else if (typeName.equals("boolean[]")) {

                    final boolean[] arrays = (boolean[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (boolean e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理char[]数组
                else if (typeName.equals("char[]")) {

                    final char[] arrays = (char[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (char e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理byte[]数组
                else if (typeName.equals("byte[]")) {

                    final byte[] arrays = (byte[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (byte e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }

                }

                // 处理Object[]数组（引用类型数组）
                else {
                    final Object[] arrays = (Object[]) obj;
                    // 如果不需要展开或是空数组，只展示摘要信息
                    if (!isExpand(deep, expand)
                        || arrays.length == 0) {

                        appendStringBuilder(buf, format("@%s[isEmpty=%s;size=%d]",
                                          typeName,
                                          arrays.length == 0,
                                          arrays.length));

                    }

                    // 展开展示数组中的每个元素
                    else {
                        appendStringBuilder(buf, format("@%s[", className));
                        for (Object e : arrays) {
                            appendStringBuilder(buf, "\n");
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            renderObject(e, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");
                        }
                        appendStringBuilder(buf, "\n");
                        for (int i = 0; i < deep; i++) {
                            appendStringBuilder(buf, TAB);
                        }
                        appendStringBuilder(buf, "]");
                    }
                }

            }


            // 处理Throwable类型（异常对象）
            else if (Throwable.class.isInstance(obj)) {

                if (!isExpand(deep, expand)) {
                    // 不展开时只显示异常类型和消息
                    appendStringBuilder(buf, format("@%s[%s]", className, obj));
                } else {
                    // 展开时显示完整的堆栈跟踪信息
                    final Throwable throwable = (Throwable) obj;
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    appendStringBuilder(buf, sw.toString());
                }

            }

            // 处理Date类型，格式化为可读的日期时间字符串
            else if (Date.class.isInstance(obj)) {
                appendStringBuilder(buf, format("@%s[%s]", className, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(obj)));
            }

            // 处理枚举类型
            else if (obj instanceof Enum<?>) {
                appendStringBuilder(buf, format("@%s[%s]", className, obj));
            }

            // 处理普通Object输出（通过反射展示字段）
            else {

                if (!isExpand(deep, expand)) {
                    // 不展开时只显示类名和toString()结果
                    appendStringBuilder(buf, format("@%s[%s]", className, obj));
                } else {
                    // 展开时通过反射获取所有字段并展示
                    appendStringBuilder(buf, format("@%s[", className));
                    final List<Field> fields;
                    Class<?> objClass = obj.getClass();
                    if (GlobalOptions.printParentFields) {
                        // 如果配置打印父类字段，则获取整个继承链的所有字段
                        fields = new ArrayList<Field>();
                        // 当父类为null的时候说明到达了最上层的父类(Object类).
                        while (objClass != null) {
                            fields.addAll(Arrays.asList(objClass.getDeclaredFields()));
                            objClass = objClass.getSuperclass();
                        }
                    } else {
                        // 只获取当前类声明的字段
                        fields = new ArrayList<Field>(Arrays.asList(objClass.getDeclaredFields()));
                    }

                    // 遍历所有字段
                    for (Field field : fields) {

                        // 设置字段可访问，打破封装限制
                        field.setAccessible(true);

                        try {

                            // 获取字段的值
                            final Object value = field.get(obj);

                            appendStringBuilder(buf, "\n");
                            // 根据深度添加缩进
                            for (int i = 0; i < deep+1; i++) {
                                appendStringBuilder(buf, TAB);
                            }
                            // 显示字段名
                            appendStringBuilder(buf, field.getName());
                            appendStringBuilder(buf, "=");
                            // 递归渲染字段的值
                            renderObject(value, deep + 1, expand, buf);
                            appendStringBuilder(buf, ",");

                        } catch (ObjectTooLargeException t) {
                            // 对象太大时停止渲染
                            buf.append("...");
                            break;
                        } catch (Throwable t) {
                            // 忽略字段访问异常（如SecurityException）
                            // ignore
                        }
                    }//for
                    appendStringBuilder(buf, "\n");
                    // 闭合括号对齐
                    for (int i = 0; i < deep; i++) {
                        appendStringBuilder(buf, TAB);
                    }
                    appendStringBuilder(buf, "]");
                }

            }
        }
    }

    /**
     * 是否展开当前深度的节点
     *
     * @param deep   当前节点的深度
     * @param expand 展开极限
     * @return true:当前节点需要展开 / false:当前节点不需要展开
     */
    private static boolean isExpand(int deep, int expand) {
        return deep < expand;
    }

    /**
     * 向StringBuilder追加字符串，并进行大小限制检查
     * 防止对象字符串过大导致内存问题
     * @param buf StringBuilder缓冲区
     * @param data 要追加的数据
     * @throws ObjectTooLargeException 如果大小超过限制则抛出此异常
     */
    private void appendStringBuilder(StringBuilder buf, String data) throws ObjectTooLargeException {
        if (buf.length() + data.length() > maxObjectLength) {
            throw new ObjectTooLargeException("Object size exceeds size limit: " + maxObjectLength);
        }
        buf.append(data);
    }

    /**
     * 自定义异常：对象太大
     * 当对象字符串表示超过最大长度限制时抛出
     */
    private static class ObjectTooLargeException extends Exception {

        public ObjectTooLargeException(String message) {
            super(message);
        }
    }

    /**
     * 规范化最大对象长度
     * 优先使用传入的limit，其次使用全局配置，最后使用默认常量
     * @param limit 用户指定的限制
     * @return 规范化后的最大对象长度
     */
    public static int normalizeMaxObjectLength(Integer limit) {
        // 如果用户指定了limit且大于0，则使用该值
        if (limit != null && limit > 0) {
            return limit;
        }
        // 否则使用全局配置的对象大小限制
        int globalLimit = GlobalOptions.objectSizeLimit;
        if (globalLimit > 0) {
            return globalLimit;
        }
        // 如果都没有配置，使用Arthas默认的HTTP内容长度限制
        return ArthasConstants.MAX_HTTP_CONTENT_LENGTH;
    }

    /**
     * 获取默认的最大对象长度
     * @return 默认的最大对象长度
     */
    private static int defaultMaxObjectLength() {
        return normalizeMaxObjectLength(null);
    }

}
