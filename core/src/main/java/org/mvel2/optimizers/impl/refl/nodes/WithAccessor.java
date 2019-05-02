package org.mvel2.optimizers.impl.refl.nodes;

import static org.mvel2.MVEL.executeSetExpression;
import static org.mvel2.util.PropertyTools.getReturnType;

import java.io.Serializable;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.ast.WithNode;
import org.mvel2.compiler.AccessorNode;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

public class WithAccessor implements AccessorNode {

    protected String nestParm;
    protected ExecutableStatement nestedStatement;
    protected WithNode.ParmValuePair[] withExpressions;
    private AccessorNode nextNode;

    public WithAccessor(ParserContext pCtx, String property, char[] expr, int start, int offset, Class ingressType) {
        pCtx.setBlockSymbols(true);

        withExpressions = WithNode.compileWithExpressions(expr, start, offset, property, ingressType, pCtx);

        pCtx.setBlockSymbols(false);
    }

    public AccessorNode getNextNode() {
        return this.nextNode;
    }

    public AccessorNode setNextNode(AccessorNode accessorNode) {
        return this.nextNode = accessorNode;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (this.nextNode == null) {
            return processWith(ctx, elCtx, variableFactory);
        } else {
            return this.nextNode.getValue(processWith(ctx, elCtx, variableFactory), elCtx, variableFactory);
        }
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return this.nextNode.setValue(processWith(ctx, elCtx, variableFactory), elCtx, variableFactory, value);
    }

    public Object processWith(Object ctx, Object thisValue, VariableResolverFactory factory) {
        for (WithNode.ParmValuePair pvp : withExpressions) {
            if (pvp.getSetExpression() != null) {
                executeSetExpression(pvp.getSetExpression(), ctx, factory, pvp.getStatement().getValue(ctx, thisValue, factory));
            } else {
                pvp.getStatement().getValue(ctx, thisValue, factory);
            }
        }

        return ctx;
    }

    public Class getKnownEgressType() {
        return Object.class;
    }

    public static final class ExecutablePairs implements Serializable {

        private Serializable setExpression;
        private ExecutableStatement statement;

        public ExecutablePairs() {
        }

        public ExecutablePairs(String parameter, ExecutableStatement statement, Class ingressType, ParserContext pCtx) {
            if (parameter != null && parameter.length() != 0) {
                this.setExpression = MVEL.compileSetExpression(parameter,
                        ingressType != null ? getReturnType(ingressType, parameter, pCtx) : Object.class, pCtx);

            }
            this.statement = statement;
        }

        public Serializable getSetExpression() {
            return setExpression;
        }

        public void setSetExpression(Serializable setExpression) {
            this.setExpression = setExpression;
        }

        public ExecutableStatement getStatement() {
            return statement;
        }

        public void setStatement(ExecutableStatement statement) {
            this.statement = statement;
        }
    }
}
