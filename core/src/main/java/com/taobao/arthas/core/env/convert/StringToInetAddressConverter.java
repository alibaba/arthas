package com.taobao.arthas.core.env.convert;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 字符串到InetAddress（互联网地址）的转换器
 *
 * <p>该转换器用于将字符串形式的主机名或IP地址转换为InetAddress对象。
 * InetAddress类用于表示互联网协议(IP)地址，可以是IPv4或IPv6地址。</p>
 *
 * <p>支持的输入格式包括：</p>
 * <ul>
 *   <li>IPv4地址：如 "192.168.1.1"</li>
 *   <li>IPv6地址：如 "2001:db8::1"</li>
 *   <li>主机名：如 "www.example.com"（会进行DNS解析）</li>
 *   <li>本地回环地址：如 "localhost" 或 "127.0.0.1"</li>
 * </ul>
 *
 * <p>注意：该类是public的，可以被外部引用使用。</p>
 */
public class StringToInetAddressConverter implements Converter<String, InetAddress> {

    /**
     * 将字符串转换为InetAddress对象
     *
     * <p>该方法使用InetAddress.getByName()静态方法实现转换。该方法会：</p>
     * <ol>
     *   <li>首先检查输入是否为IP地址格式的字符串，如果是则直接创建对应的InetAddress对象</li>
     *   <li>如果不是IP地址格式，则将其视为主机名，通过DNS系统查询解析对应的IP地址</li>
     * </ol>
     *
     * <p>示例用法：</p>
     * <pre>
     * StringToInetAddressConverter converter = new StringToInetAddressConverter();
     * InetAddress addr1 = converter.convert("192.168.1.1", InetAddress.class);
     * InetAddress addr2 = converter.convert("www.example.com", InetAddress.class);
     * </pre>
     *
     * @param source 源字符串，可以是IP地址或主机名
     * @param targetType 目标类型的Class对象，此处为InetAddress.class（实际上参数不会被使用，仅用于接口一致性）
     * @return 包含主机名和对应IP地址的InetAddress对象
     * @throws IllegalArgumentException 如果source字符串无法解析为有效的主机名或IP地址，
     *                                  或者DNS解析失败。原始的UnknownHostException异常会被包装在其中
     * @throws NullPointerException 如果source为null
     */
    @Override
    public InetAddress convert(String source, Class<InetAddress> targetType) {
        try {
            // 调用InetAddress.getByName()方法将字符串转换为InetAddress对象
            // 该方法会处理主机名解析、IP地址验证等逻辑
            // 如果是IP地址，直接返回；如果是主机名，进行DNS查询
            return InetAddress.getByName(source);
        } catch (UnknownHostException e) {
            // 如果主机名无法解析（DNS查询失败或无效的主机名/IP格式），
            // 将UnknownHostException包装为IllegalArgumentException抛出
            // 这样符合转换器接口的异常处理规范
            throw new IllegalArgumentException("Invalid InetAddress value '" + source + "'", e);
        }
    }

}
