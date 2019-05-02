package org.mvel2.ast;

import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;

public class IntMult extends BinaryOperation implements IntOptimized {

    public IntMult(ASTNode left, ASTNode right, ParserContext pCtx) {
        super(Operator.MULT, pCtx);
        this.left = left;
        this.right = right;
    }

    @Override
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ((Integer) left.getReducedValueAccelerated(ctx, thisValue, factory))
                * ((Integer) right.getReducedValueAccelerated(ctx, thisValue, factory));
    }

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return ((Integer) left.getReducedValue(ctx, thisValue, factory)) * ((Integer) right.getReducedValue(ctx, thisValue, factory));
    }

    @Override
    public Class getEgressType() {
        return Integer.class;
    }
}