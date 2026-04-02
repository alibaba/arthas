
package com.taobao.arthas.core.env.convert;

/**
 * 字符串到Long（长整数）类型的转换器
 *
 * <p>该转换器用于将字符串形式的数字转换为Long对象。</p>
 *
 * <p>支持的输入格式：</p>
 * <ul>
 *   <li>十进制整数：如 "123"、"-456"</li>
 *   <li>支持正负号：以"+"或"-"开头</li>
 * </ul>
 *
 * <p>注意：该转换器使用Long.parseLong()方法，要求输入必须是有效的十进制整数格式。
 * 不支持其他进制（如十六进制0x开头、八进制0开头等）的字符串。</p>
 *
 * <p>数值范围：必须在Long.MIN_VALUE（-9223372036854775808）到Long.MAX_VALUE（9223372036854775807）之间。
 * 相比Integer类型，Long类型可以表示更大范围的整数，适用于需要处理大数值的场景。</p>
 */
final class StringToLongConverter implements Converter<String, Long> {

    /**
     * 将字符串转换为Long对象
     *
     * <p>该方法使用Long.parseLong()方法实现转换，该方法会：</p>
     * <ol>
     *   <li>解析字符串中的字符，验证是否为有效的十进制数字格式</li>
     *   <li>处理可选的正负号（+或-）</li>
     *   <li>将解析结果转换为long基本类型值</li>
     *   <li>自动装箱为Long对象返回</li>
     * </ol>
     *
     * <p>示例用法：</p>
     * <pre>
     * StringToLongConverter converter = new StringToLongConverter();
     * Long num1 = converter.convert("123", Long.class);                    // 结果：123L
     * Long num2 = converter.convert("-456", Long.class);                   // 结果：-456L
     * Long num3 = converter.convert("9223372036854775807", Long.class);    // 结果：9223372036854775807L (Long最大值)
     * </pre>
     *
     * @param source 源字符串，必须是有效的十进制整数格式
     * @param targetType 目标类型的Class对象，此处为Long.class（实际上参数不会被使用，仅用于接口一致性）
     * @return 解析后的Long对象
     * @throws NumberFormatException 如果source字符串不是有效的整数字符串格式，
     *                                或者数值超出了Long类型的表示范围
     * @throws NullPointerException 如果source为null
     */
    @Override
    public Long convert(String source, Class<Long> targetType) {
        // 调用Long.parseLong()方法将字符串解析为long基本类型
        // 该方法会自动将结果装箱为Long对象返回
        // 如果字符串格式不正确或超出long范围，会抛出NumberFormatException
        return Long.parseLong(source);
    }
}
