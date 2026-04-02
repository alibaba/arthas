package com.taobao.arthas.core.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;
import static com.taobao.arthas.core.util.ArthasCheckUtils.isIn;
import static com.taobao.arthas.core.util.StringUtils.isBlank;

/**
 * Feature编解器(线程安全)
 * <p>
 * 该类用于封装系统内部features/attribute等扩展字段的管理，提供键值对与字符串之间的编解码功能。
 * 支持自定义分隔符，并提供转义机制以处理特殊字符。编解码过程是可逆的，保证了数据的完整性。
 * </p>
 * <p>
 * 编码格式示例：使用分号作为KV片段分隔符，等号作为KV分隔符
 * ;key1=value1;key2=value2;
 * </p>
 * <p>
 * 该类是线程安全的，可以在多线程环境中安全使用。
 * </p>
 * Created by dukun on 15/3/31.
 */
public class FeatureCodec {
    // 默认的命令行编解码器实例，使用分号作为KV片段分隔符，等号作为KV分隔符
    // 这个预定义的实例可以直接用于大多数命令行场景的编解码需求
    public final static FeatureCodec DEFAULT_COMMANDLINE_CODEC = new FeatureCodec(';', '=');

    /**
     * KV片段分割符
     * <p>
     * KV片段定义为一个完整的KV对，例如字符串; k1=v1;k2=v2;
     * 其中;即为KV片段分隔符，用于分隔不同的键值对
     * </p>
     */
    private final char kvSegmentSeparator;

    /**
     * KV分割符
     * <p>
     * KV定义为一个KV对区分K和V的分割符号，例如字符串k1=v1
     * 其中=即为KV分隔符，用于分隔键和值
     * </p>
     */
    private final char kvSeparator;

    /**
     * 转义前缀符
     * <p>
     * 当键或值中包含分隔符时，使用此字符进行转义，保证编解码的正确性
     * </p>
     */
    private static final char ESCAPE_PREFIX_CHAR = '\\';

    /**
     * 使用指定的KV分割符构造FeatureParser
     * <p>
     * 构造函数会验证分隔符不能与转义前缀符相同，否则会抛出异常
     * </p>
     *
     * @param kvSegmentSeparator KV对之间的分隔符，用于分隔不同的键值对
     * @param kvSeparator        K与V之间的分隔符，用于分隔键和值
     * @throws IllegalArgumentException 如果分隔符与转义前缀符相同
     */
    public FeatureCodec(final char kvSegmentSeparator, final char kvSeparator) {

        // 分隔符禁止与转义前缀符相等，否则会导致转义逻辑混乱
        if (isIn(ESCAPE_PREFIX_CHAR, kvSegmentSeparator, kvSeparator)) {
            throw new IllegalArgumentException("separator can not init to '" + ESCAPE_PREFIX_CHAR + "'.");
        }

        this.kvSegmentSeparator = kvSegmentSeparator;
        this.kvSeparator = kvSeparator;
    }

    /**
     * map集合转换到feature字符串
     * <p>
     * 将Map集合编码为feature字符串格式，字符串以KV片段分隔符开头和结尾，
     * 每个键值对之间用KV片段分隔符分隔，键和值之间用KV分隔符分隔。
     * 如果键或值中包含特殊字符（分隔符或转义符），会自动进行转义处理。
     * </p>
     *
     * @param map 要转换的map集合，键和值都是字符串类型
     * @return feature字符串，格式如：;key1=value1;key2=value2;
     */
    public String toString(final Map<String, String> map) {

        // 创建StringBuilder，并以KV片段分隔符开头
        final StringBuilder featureSB = new StringBuilder().append(kvSegmentSeparator);

        // 如果map为null或空，直接返回只有分隔符的字符串
        if (null == map
                || map.isEmpty()) {
            return featureSB.toString();
        }

        // 遍历map中的每个条目
        for (Map.Entry<String, String> entry : map.entrySet()) {

            // 对每个键值对进行编码：键 + KV分隔符 + 值 + KV片段分隔符
            featureSB
                    .append(escapeEncode(entry.getKey()))      // 对键进行转义编码
                    .append(kvSeparator)                       // 添加KV分隔符
                    .append(escapeEncode(entry.getValue()))    // 对值进行转义编码
                    .append(kvSegmentSeparator)                // 添加KV片段分隔符
            ;

        }

        return featureSB.toString();
    }


    /**
     * feature字符串转换到map集合
     * <p>
     * 将feature格式的字符串解码为Map集合。该方法会：
     * 1. 使用KV片段分隔符分割字符串，得到各个键值对片段
     * 2. 对每个片段使用KV分隔符分割，得到键和值
     * 3. 对键和值进行转义解码
     * 4. 过滤掉空键、空值或格式不正确的片段
     * </p>
     *
     * @param featureString 要解析的feature字符串
     * @return 解析后的map集合，如果输入字符串为空则返回空map
     */
    public Map<String, String> toMap(final String featureString) {

        // 创建用于存储结果的HashMap
        final Map<String, String> map = new HashMap<String, String>();

        // 如果输入字符串为空，直接返回空map
        if (isBlank(featureString)) {
            return map;
        }

        // 使用KV片段分隔符分割字符串
        for (String kv : escapeSplit(featureString, kvSegmentSeparator)) {

            // 过滤掉为空的字符串片段
            if (isBlank(kv)) {
                continue;
            }

            // 使用KV分隔符分割键值对
            final String[] ar = escapeSplit(kv, kvSeparator);
            // 过滤掉不符合K:V格式的情况（即分割后不等于2个元素）
            if (ar.length != 2) {
                continue;
            }

            final String k = ar[0];
            final String v = ar[1];
            // 只有当键和值都不为空时，才添加到map中
            if (!isBlank(k)
                    && !isBlank(v)) {
                map.put(escapeDecode(k), escapeDecode(v));
            }

        }

        return map;
    }

    /**
     * 转义编码
     * <p>
     * 对字符串中的特殊字符（KV片段分隔符、KV分隔符、转义前缀符）进行转义处理。
     * 转义方式是在特殊字符前添加转义前缀符。
     * </p>
     * <p>
     * 例如：如果分隔符是;和=，则字符串"a;b=c"会被编码为"a\;b\=c"
     * </p>
     *
     * @param string 原始字符串
     * @return 转义编码后的字符串
     */
    private String escapeEncode(final String string) {
        final StringBuilder returnSB = new StringBuilder();
        // 遍历字符串中的每个字符
        for (final char c : string.toCharArray()) {
            // 如果字符是特殊字符（分隔符或转义符），则在其前面添加转义前缀符
            if (isIn(c, kvSegmentSeparator, kvSeparator, ESCAPE_PREFIX_CHAR)) {
                returnSB.append(ESCAPE_PREFIX_CHAR);
            }
            returnSB.append(c);
        }

        return returnSB.toString();
    }

    /**
     * 转义解码
     * <p>
     * 对经过转义编码的字符串进行解码，恢复原始字符串。
     * 该方法会识别转义序列（转义前缀符+特殊字符），并将其替换为原始字符。
     * </p>
     * <p>
     * 解码规则：
     * 1. 遇到转义前缀符且后面跟着特殊字符时，只保留特殊字符
     * 2. 遇到转义前缀符但后面不是特殊字符时，保留转义前缀符和后面的字符
     * 3. 其他情况直接保留字符
     * </p>
     *
     * @param string 编码字符串
     * @return 转义解码后的字符串
     */
    private String escapeDecode(String string) {

        final StringBuilder segmentSB = new StringBuilder();
        final int stringLength = string.length();

        // 遍历字符串中的每个字符
        for (int index = 0; index < stringLength; index++) {

            final char c = string.charAt(index);

            // 检查是否是转义前缀符，并且不是最后一个字符
            if (isEquals(c, ESCAPE_PREFIX_CHAR)
                    && index < stringLength - 1) {

                // 获取下一个字符
                final char nextChar = string.charAt(++index);

                // 下一个字符是需要转义的特殊字符（分隔符或转义符本身）
                if (isIn(nextChar, kvSegmentSeparator, kvSeparator, ESCAPE_PREFIX_CHAR)) {
                    // 只保留特殊字符，去掉转义前缀符
                    segmentSB.append(nextChar);
                }

                // 如果不是需要转义的字符，则需要两个都保留（转义前缀符和下一个字符）
                else {
                    segmentSB.append(c);
                    segmentSB.append(nextChar);
                }
            } else {
                // 普通字符，直接添加
                segmentSB.append(c);
            }

        }

        return segmentSB.toString();
    }

    /**
     * 编码字符串拆分
     * <p>
     * 根据指定的分割符对编码字符串进行拆分，同时正确处理转义字符。
     * 该方法会跳过被转义的分割符，只分割真正的分割符。
     * </p>
     * <p>
     * 算法思路：
     * 1. 使用栈来临时存储字符
     * 2. 遇到转义前缀符时，将其和下一个字符一起压入栈
     * 3. 遇到分割符时，标记为一个片段结束
     * 4. 遇到其他字符时，直接压入栈
     * 5. 当片段结束或到达字符串末尾时，将栈中元素弹出并反转，得到一个片段
     * </p>
     *
     * @param string          编码字符串
     * @param splitEscapeChar 分割符
     * @return 拆分后的字符串数组
     */
    private String[] escapeSplit(String string, char splitEscapeChar) {

        final ArrayList<String> segmentArrayList = new ArrayList<String>();
        final Stack<Character> decodeStack = new Stack<Character>();
        final int stringLength = string.length();

        // 遍历字符串中的每个字符
        for (int index = 0; index < stringLength; index++) {

            boolean isArchive = false;

            final char c = string.charAt(index);

            // 匹配到转义前缀符
            if (isEquals(c, ESCAPE_PREFIX_CHAR)) {

                // 将转义前缀符压入栈
                decodeStack.push(c);
                // 如果不是最后一个字符，将下一个字符也压入栈
                if (index < stringLength - 1) {
                    final char nextChar = string.charAt(++index);
                    decodeStack.push(nextChar);
                }

            }

            // 匹配到分割符（未转义的）
            else if (isEquals(c, splitEscapeChar)) {
                // 标记当前片段结束
                isArchive = true;
            }

            // 匹配到其他字符
            else {
                // 直接压入栈
                decodeStack.push(c);
            }

            // 如果是分割符或到达字符串末尾，则处理当前片段
            if (isArchive
                    || index == stringLength - 1) {
                final StringBuilder segmentSB = new StringBuilder(decodeStack.size());
                // 将栈中所有元素弹出，得到逆序的字符串
                while (!decodeStack.isEmpty()) {
                    segmentSB.append(decodeStack.pop());
                }

                // 添加到结果列表：先反转恢复正确顺序，然后转换为字符串并去除首尾空格
                segmentArrayList.add(
                        segmentSB
                                .reverse()  // 因为堆栈中是逆序的,所以需要对逆序的字符串再次逆序
                                .toString() // toString
                                .trim()     // 考虑到字符串片段可能会出现首尾空格的场景，这里做一个过滤
                );
            }

        }

        // 转换为字符串数组返回
        return segmentArrayList.toArray(new String[0]);
    }


}