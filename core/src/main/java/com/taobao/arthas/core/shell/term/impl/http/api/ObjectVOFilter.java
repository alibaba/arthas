package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * ObjectVO 值过滤器，用于在 JSON 序列化时对 ObjectVO 对象进行特殊处理。
 *
 * <p>该类实现了 FastJSON2 的 ValueFilter 接口，用于在 JSON 序列化过程中拦截和处理 ObjectVO 对象。
 * 当序列化遇到 ObjectVO 对象时，会根据其是否需要展开来决定如何转换：
 * <ul>
 *   <li>如果需要展开（needExpand() 返回 true），则使用 ObjectView 绘制对象的结构化视图</li>
 *   <li>如果不需要展开，则直接使用对象的 toString() 结果</li>
 * </ul>
 *
 * <p>这个过滤器主要用于 HTTP 终端实现中，在将命令结果序列化为 JSON 响应时，
 * 对 ObjectVO 对象进行格式化处理，以便在前端更好地展示对象内容。
 *
 * @author hengyunabc 2022-08-24
 */
public class ObjectVOFilter implements ValueFilter {

    /**
     * 对 JSON 序列化过程中的值进行过滤和转换。
     *
     * <p>FastJSON2 在序列化每个字段时会调用此方法，允许我们对特定类型的值进行自定义处理。
     *
     * @param object 包含当前字段的对象实例
     * @param name   当前字段的名称
     * @param value  当前字段的值
     * @return 过滤后的值，如果值是 ObjectVO 对象，则返回其格式化后的字符串表示
     */
    @Override
    public Object apply(Object object, String name, Object value) {
        // 检查当前值是否为 ObjectVO 类型
        if (value instanceof ObjectVO) {
            ObjectVO vo = (ObjectVO) value;

            // 根据 ObjectVO 是否需要展开来决定如何处理：
            // 1. 如果需要展开（needExpand() 返回 true），使用 ObjectView 绘制对象的结构化视图
            //    ObjectView 会生成对象的层次化文本表示，包括类的字段、值等信息
            // 2. 如果不需要展开，直接使用原值的 toString() 结果
            String resultStr = StringUtils.objectToString(vo.needExpand() ? new ObjectView(vo).draw() : value);

            // 返回处理后的字符串结果
            return resultStr;
        }

        // 如果不是 ObjectVO 对象，直接返回原值，不做任何处理
        return value;
    }

}
