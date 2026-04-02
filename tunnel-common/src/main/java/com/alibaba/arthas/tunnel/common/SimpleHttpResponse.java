package com.alibaba.arthas.tunnel.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单HTTP响应类
 *
 * 该类用于封装HTTP响应信息，包括状态码、响应头和响应体内容。
 * 它实现了Serializable接口，支持序列化和反序列化，便于在网络中传输。
 *
 * 主要功能：
 * 1. 存储HTTP响应的状态码、响应头和响应体
 * 2. 支持将响应对象序列化为字节数组
 * 3. 支持从字节数组反序列化为响应对象
 * 4. 在反序列化时进行白名单检查，防止反序列化漏洞攻击
 *
 * 安全特性：
 * - 在反序列化时使用白名单机制，只允许特定的类被反序列化
 * - 白名单包括基本类型和SimpleHttpResponse本身
 * - 防止恶意类的反序列化执行
 *
 * @author hengyunabc 2020-10-22
 *
 */
public class SimpleHttpResponse implements Serializable {
    /**
     * 序列化版本UID，用于确保序列化兼容性
     */
    private static final long serialVersionUID = 1L;

    /**
     * 反序列化白名单，只允许以下类被反序列化
     *
     * 包含的类：
     * - byte[]: 字节数组，用于存储响应内容
     * - String: 字符串类
     * - Map: Map接口
     * - HashMap: HashMap实现类，用于存储响应头
     * - SimpleHttpResponse: 本类本身
     */
    private static final List<String> whitelist = Arrays.asList(byte[].class.getName(), String.class.getName(),
            Map.class.getName(), HashMap.class.getName(), SimpleHttpResponse.class.getName());

    /**
     * HTTP响应状态码，默认为200（成功）
     */
    private int status = 200;

    /**
     * HTTP响应头集合，使用Map存储键值对
     * 键为响应头的名称，值为响应头的值
     */
    private Map<String, String> headers = new HashMap<String, String>();

    /**
     * HTTP响应体内容，以字节数组形式存储
     */
    private byte[] content;

    /**
     * 添加响应头
     *
     * 将指定的响应头键值对添加到headers集合中。
     * 如果已存在相同键的响应头，将被覆盖。
     *
     * @param key 响应头的名称（例如："Content-Type"）
     * @param value 响应头的值（例如："application/json"）
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 获取所有响应头
     *
     * 返回包含所有响应头的Map集合。
     * 返回的Map是可修改的，直接操作会影响本对象的headers。
     *
     * @return 响应头Map集合，键为响应头名称，值为响应头值
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * 设置响应头集合
     *
     * 使用提供的Map替换当前的响应头集合。
     *
     * @param headers 新的响应头Map集合
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * 获取响应体内容
     *
     * 返回HTTP响应体的字节数组形式。
     *
     * @return 响应体内容的字节数组，如果未设置则返回null
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * 设置响应体内容
     *
     * 设置HTTP响应体的字节数组内容。
     *
     * @param content 响应体内容的字节数组
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * 获取HTTP状态码
     *
     * 返回HTTP响应的状态码（如200、404、500等）。
     *
     * @return HTTP状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 设置HTTP状态码
     *
     * 设置HTTP响应的状态码。
     *
     * @param status HTTP状态码（例如：200表示成功，404表示未找到）
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * 将SimpleHttpResponse对象序列化为字节数组
     *
     * 该方法使用Java的对象序列化机制，将SimpleHttpResponse对象
     * 转换为字节数组，便于在网络中传输或持久化存储。
     *
     * 序列化过程：
     * 1. 创建ByteArrayOutputStream作为输出目标
     * 2. 创建ObjectOutputStream包装字节输出流
     * 3. 写入对象并刷新
     * 4. 返回字节数组
     *
     * @param response 要序列化的SimpleHttpResponse对象
     * @return 序列化后的字节数组
     * @throws IOException 当序列化过程中发生IO错误
     */
    public static byte[] toBytes(SimpleHttpResponse response) throws IOException {
        // 创建字节数组输出流，用于存储序列化后的数据
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 使用try-with-resources确保ObjectOutputStream被正确关闭
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            // 将响应对象写入输出流
            out.writeObject(response);
            // 刷新输出流，确保所有数据都被写入
            out.flush();
            // 返回序列化后的字节数组
            return bos.toByteArray();
        }
    }

    /**
     * 从字节数组反序列化为SimpleHttpResponse对象
     *
     * 该方法使用Java的对象反序列化机制，将字节数组转换回SimpleHttpResponse对象。
     * 为了安全起见，该方法实现了白名单机制，只允许白名单中的类被反序列化。
     *
     * 反序列化过程：
     * 1. 创建ByteArrayInputStream作为输入源
     * 2. 创建ObjectInputStream并重写resolveClass方法
     * 3. 在resolveClass中检查类是否在白名单中
     * 4. 如果类不在白名单中，抛出InvalidClassException
     * 5. 读取并返回对象
     *
     * 安全机制：
     * - 使用白名单过滤可反序列化的类
     * - 防止反序列化漏洞攻击
     * - 只允许基本类型和本类本身被反序列化
     *
     * @param bytes 包含序列化对象的字节数组
     * @return 反序列化后的SimpleHttpResponse对象
     * @throws IOException 当反序列化过程中发生IO错误
     * @throws ClassNotFoundException 当白名单检查失败或其他类相关错误
     */
    public static SimpleHttpResponse fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        // 创建字节数组输入流，包装要反序列化的字节数组
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        // 使用try-with-resources确保ObjectInputStream被正确关闭
        try (ObjectInputStream in = new ObjectInputStream(bis) {
            // 重写resolveClass方法，实现白名单检查机制
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                // 检查要反序列化的类是否在白名单中
                if (!whitelist.contains(desc.getName())) {
                    // 如果不在白名单中，抛出异常，防止恶意类被反序列化
                    throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
                }
                // 如果在白名单中，调用父类方法正常解析类
                return super.resolveClass(desc);
            }
        }) {
            // 读取并返回反序列化后的对象
            return (SimpleHttpResponse) in.readObject();
        }
    }

}
