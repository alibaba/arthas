package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.GlobalOptions;

import ognl.ObjectPropertyAccessor;
import ognl.OgnlContext;
import ognl.OgnlException;

/**
 * @author hengyunabc 2022-03-24
 */
public class ArthasObjectPropertyAccessor extends ObjectPropertyAccessor {

    @Override
    public Object setPossibleProperty(OgnlContext context, Object target, String name, Object value) throws OgnlException {
        if (GlobalOptions.strict) {
            throw new IllegalAccessError(GlobalOptions.STRICT_MESSAGE);
        }
        return super.setPossibleProperty(context, target, name, value);
    }

}
