package org.mvel2.ast;

import static org.mvel2.util.ParseTools.boxPrimitive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ParseTools;

public class Sign extends ASTNode {

    private Signer signer;
    private ExecutableStatement stmt;

    public Sign(char[] expr, int start, int end, int fields, ParserContext pCtx) {
        super(pCtx);
        this.expr = expr;
        this.start = start + 1;
        this.offset = end - 1;
        this.fields = fields;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            stmt = (ExecutableStatement) ParseTools.subCompileExpression(expr, this.start, this.offset, pCtx);

            egressType = stmt.getKnownEgressType();

            if (egressType != null && egressType != Object.class) {
                initSigner(egressType);
            }
        }
    }

    public ExecutableStatement getStatement() {
        return stmt;
    }

    @Override
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return sign(stmt.getValue(ctx, thisValue, factory));
    }

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return sign(MVEL.eval(expr, start, offset, thisValue, factory));
    }

    private Object sign(Object o) {
        if (o == null) return null;
        if (signer == null) {
            if (egressType == null || egressType == Object.class) egressType = o.getClass();
            initSigner(egressType);
        }
        return signer.sign(o);
    }

    private void initSigner(Class type) {
        if (Integer.class.isAssignableFrom(type = boxPrimitive(type))) signer = new IntegerSigner();
        else if (Double.class.isAssignableFrom(type)) signer = new DoubleSigner();
        else if (Long.class.isAssignableFrom(type)) signer = new LongSigner();
        else if (Float.class.isAssignableFrom(type)) signer = new FloatSigner();
        else if (Short.class.isAssignableFrom(type)) signer = new ShortSigner();
        else if (BigInteger.class.isAssignableFrom(type)) signer = new BigIntSigner();
        else if (BigDecimal.class.isAssignableFrom(type)) signer = new BigDecSigner();
        else {
            throw new CompileException("illegal use of '-': cannot be applied to: " + type.getName(), expr, start);
        }

    }

    @Override
    public boolean isIdentifier() {
        return false;
    }

    private interface Signer extends Serializable {

        public Object sign(Object o);
    }

    private class IntegerSigner implements Signer {

        public Object sign(Object o) {
            return -((Integer) o);
        }
    }

    private class ShortSigner implements Signer {

        public Object sign(Object o) {
            return -((Short) o);
        }
    }

    private class LongSigner implements Signer {

        public Object sign(Object o) {
            return -((Long) o);
        }
    }

    private class DoubleSigner implements Signer {

        public Object sign(Object o) {
            return -((Double) o);
        }
    }

    private class FloatSigner implements Signer {

        public Object sign(Object o) {
            return -((Float) o);
        }
    }

    private class BigIntSigner implements Signer {

        public Object sign(Object o) {
            return new BigInteger(String.valueOf(-(((BigInteger) o).longValue())));
        }
    }

    private class BigDecSigner implements Signer {

        public Object sign(Object o) {
            return new BigDecimal(-((BigDecimal) o).doubleValue());
        }
    }
}
