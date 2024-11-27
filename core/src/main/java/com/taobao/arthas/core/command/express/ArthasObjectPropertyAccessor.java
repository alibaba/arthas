package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.GlobalOptions;

import ognl.ObjectPropertyAccessor;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;

import java.util.Map;

/**
 * @author hengyunabc 2022-03-24
 */
public class ArthasObjectPropertyAccessor extends ObjectPropertyAccessor {

    @Override
    public Object getPossibleProperty(Map context, Object target, String name) throws OgnlException {
        Object result;
        try {
            result = OgnlRuntime.getFieldValue((OgnlContext) context, target, name, true);
        } catch (Exception ex) {
            throw new OgnlException(name, ex);
        }

        return result;
    }

    @Override
    public Object setPossibleProperty(Map context, Object target, String name, Object value) throws OgnlException {
        if (GlobalOptions.strict) {
            throw new IllegalAccessError(GlobalOptions.STRICT_MESSAGE);
        }
        return super.setPossibleProperty(context, target, name, value);
    }

}
