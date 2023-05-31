package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.fastjson2.filter.ValueFilter;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * @author hengyunabc 2022-08-24
 *
 */
public class ObjectVOFilter implements ValueFilter {

    @Override
    public Object apply(Object object, String name, Object value) {
        if (value instanceof ObjectVO) {
            ObjectVO vo = (ObjectVO) value;
            String resultStr = StringUtils.objectToString(vo.needExpand() ? new ObjectView(vo).draw() : value);
            return resultStr;
        }
        return value;
    }

}
