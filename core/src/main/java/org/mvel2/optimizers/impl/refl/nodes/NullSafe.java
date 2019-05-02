package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

public class NullSafe implements AccessorNode {

    private AccessorNode nextNode;
    private char[] expr;
    private int start;
    private int offset;
    private ParserContext pCtx;

    public NullSafe(char[] expr, int start, int offset, ParserContext pCtx) {
        this.expr = expr;
        this.start = start;
        this.offset = offset;
        this.pCtx = pCtx;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (ctx == null) return null;
        if (nextNode == null) {
            final Accessor a = OptimizerFactory.getAccessorCompiler(OptimizerFactory.SAFE_REFLECTIVE).optimizeAccessor(pCtx, expr, start,
                    offset, ctx, elCtx, variableFactory, true, ctx.getClass());

            nextNode = new AccessorNode() {

                public AccessorNode getNextNode() {
                    return null;
                }

                public AccessorNode setNextNode(AccessorNode accessorNode) {
                    return null;
                }

                public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
                    return a.getValue(ctx, elCtx, variableFactory);
                }

                public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
                    return a.setValue(ctx, elCtx, variableFactory, value);
                }

                public Class getKnownEgressType() {
                    return a.getKnownEgressType();
                }
            };

        }
        //   else {
        return nextNode.getValue(ctx, elCtx, variableFactory);
        //    }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        if (ctx == null) return null;
        return nextNode.setValue(ctx, elCtx, variableFactory, value);
    }

    public AccessorNode getNextNode() {
        return nextNode;
    }

    public AccessorNode setNextNode(AccessorNode accessorNode) {
        return this.nextNode = accessorNode;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }
}
