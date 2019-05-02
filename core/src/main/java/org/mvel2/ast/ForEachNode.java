/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mvel2.ast;

import static org.mvel2.util.ParseTools.createStringTrimmed;
import static org.mvel2.util.ParseTools.getBaseComponentType;
import static org.mvel2.util.ParseTools.subCompileExpression;

import java.lang.reflect.Array;

import org.mvel2.CompileException;
import org.mvel2.DataConversion;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel2.integration.impl.ItemResolverFactory;
import org.mvel2.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ForEachNode extends BlockNode {

    private static final int ITERABLE = 0;
    private static final int ARRAY = 1;
    private static final int CHARSEQUENCE = 2;
    private static final int INTEGER = 3;
    protected String item;
    protected Class itemType;
    protected ExecutableStatement condition;
    private int type = -1;

    public ForEachNode(char[] expr, int start, int offset, int blockStart, int blockOffset, int fields, ParserContext pCtx) {
        super(pCtx);

        handleCond(this.expr = expr, this.start = start, this.offset = offset, this.fields = fields, pCtx);
        this.blockStart = blockStart;
        this.blockOffset = blockOffset;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            if (pCtx.isStrictTypeEnforcement() && itemType != null) {
                pCtx = pCtx.createSubcontext();
                pCtx.addInput(item, itemType);
            }

            pCtx.pushVariableScope();
            pCtx.makeVisible(item);

            this.compiledBlock = (ExecutableStatement) subCompileExpression(expr, blockStart, blockOffset, pCtx);

            pCtx.popVariableScope();
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, new DefaultLocalVariableResolverFactory(factory));

        Object iterCond = condition.getValue(ctx, thisValue, factory);

        if (type == -1) {
            determineIterType(iterCond.getClass());
        }

        Object v;
        switch (type) {
            case ARRAY:
                int len = Array.getLength(iterCond);
                for (int i = 0; i < len; i++) {
                    itemR.setValue(Array.get(iterCond, i));
                    v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                    if (itemFactory.tiltFlag()) return v;
                }
                break;
            case CHARSEQUENCE:
                for (Object o : iterCond.toString().toCharArray()) {
                    itemR.setValue(o);
                    v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                    if (itemFactory.tiltFlag()) return v;
                }
                break;
            case INTEGER:
                int max = (Integer) iterCond + 1;
                for (int i = 1; i != max; i++) {
                    itemR.setValue(i);
                    v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                    if (itemFactory.tiltFlag()) return v;
                }
                break;

            case ITERABLE:
                for (Object o : (Iterable) iterCond) {
                    itemR.setValue(o);
                    v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                    if (itemFactory.tiltFlag()) return v;
                }

                break;
        }

        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, new DefaultLocalVariableResolverFactory(factory));

        Object iterCond = MVEL.eval(expr, start, offset, thisValue, factory);

        if (itemType != null && itemType.isArray()) enforceTypeSafety(itemType, getBaseComponentType(iterCond.getClass()));

        this.compiledBlock = (ExecutableStatement) subCompileExpression(expr, blockStart, blockOffset, pCtx);

        Object v;
        if (iterCond instanceof Iterable) {
            for (Object o : (Iterable) iterCond) {
                itemR.setValue(o);
                v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                if (itemFactory.tiltFlag()) return v;
            }
        } else if (iterCond != null && iterCond.getClass().isArray()) {
            int len = Array.getLength(iterCond);
            for (int i = 0; i < len; i++) {
                itemR.setValue(Array.get(iterCond, i));
                v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                if (itemFactory.tiltFlag()) return v;
            }
        } else if (iterCond instanceof CharSequence) {
            for (Object o : iterCond.toString().toCharArray()) {
                itemR.setValue(o);
                v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                if (itemFactory.tiltFlag()) return v;
            }
        } else if (iterCond instanceof Integer) {
            int max = (Integer) iterCond + 1;
            for (int i = 1; i != max; i++) {
                itemR.setValue(i);
                v = compiledBlock.getValue(ctx, thisValue, itemFactory);
                if (itemFactory.tiltFlag()) return v;
            }
        } else {
            throw new CompileException("non-iterable type: " + (iterCond != null ? iterCond.getClass().getName() : "null"), expr, start);
        }

        return null;
    }

    private void handleCond(char[] condition, int start, int offset, int fields, ParserContext pCtx) {
        int cursor = start;
        int end = start + offset;
        while (cursor < end && condition[cursor] != ':')
            cursor++;

        if (cursor == end || condition[cursor] != ':') throw new CompileException("expected : in foreach", condition, cursor);

        int x;
        if ((x = (item = createStringTrimmed(condition, start, cursor - start)).indexOf(' ')) != -1) {
            String tk = new String(condition, start, x).trim();
            try {
                itemType = ParseTools.findClass(null, tk, pCtx);
                item = new String(condition, start + x, (cursor - start) - x).trim();

            } catch (ClassNotFoundException e) {
                throw new CompileException("cannot resolve identifier: " + tk, condition, start);
            }
        }

        // this.start = ++cursor;

        this.start = cursor + 1;
        this.offset = offset - (cursor - start) - 1;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            Class egress = (this.condition = (ExecutableStatement) subCompileExpression(expr, this.start, this.offset, pCtx))
                    .getKnownEgressType();

            if (itemType != null && egress.isArray()) {
                enforceTypeSafety(itemType, getBaseComponentType(this.condition.getKnownEgressType()));
            } else if (pCtx.isStrongTyping()) {
                determineIterType(egress);
            }
        }
    }

    private void determineIterType(Class t) {
        if (Iterable.class.isAssignableFrom(t)) {
            type = ITERABLE;
        } else if (t.isArray()) {
            type = ARRAY;
        } else if (CharSequence.class.isAssignableFrom(t)) {
            type = CHARSEQUENCE;
        } else if (Integer.class.isAssignableFrom(t)) {
            type = INTEGER;
        } else {
            throw new CompileException("non-iterable type: " + t.getName(), expr, start);
        }
    }

    private void enforceTypeSafety(Class required, Class actual) {
        if (!required.isAssignableFrom(actual) && !DataConversion.canConvert(actual, required)) {
            throw new CompileException(
                    "type mismatch in foreach: expected: " + required.getName() + "; but found: " + getBaseComponentType(actual), expr,
                    start);
        }
    }
}
