package org.mvel2.ast;

import static org.mvel2.util.Soundex.soundex;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.CompilerTools;

public class Soundslike extends ASTNode {

    private ASTNode stmt;
    private ASTNode soundslike;

    public Soundslike(ASTNode stmt, ASTNode clsStmt, ParserContext pCtx) {
        super(pCtx);
        this.stmt = stmt;
        this.soundslike = clsStmt;
        CompilerTools.expectType(pCtx, clsStmt, String.class, true);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        String str1 = String.valueOf(soundslike.getReducedValueAccelerated(ctx, thisValue, factory));
        String str2 = (String) stmt.getReducedValueAccelerated(ctx, thisValue, factory);
        return str1 == null ? str2 == null : (str2 == null ? false : soundex(str1).equals(soundex(str2)));
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            String i = String.valueOf(soundslike.getReducedValue(ctx, thisValue, factory));
            if (i == null) throw new ClassCastException();

            String x = (String) stmt.getReducedValue(ctx, thisValue, factory);
            if (x == null) throw new CompileException("not a string: " + stmt.getName(), stmt.getExpr(), stmt.getStart());

            return soundex(i).equals(soundex(x));
        } catch (ClassCastException e) {
            throw new CompileException("not a string: " + soundslike.getName(), soundslike.getExpr(), soundslike.getStart());
        }

    }

    public Class getEgressType() {
        return Boolean.class;
    }

    public ASTNode getStatement() {
        return stmt;
    }

    public ASTNode getSoundslike() {
        return soundslike;
    }
}
