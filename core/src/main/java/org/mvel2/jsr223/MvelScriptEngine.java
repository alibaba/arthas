package org.mvel2.jsr223;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.mvel2.MVEL;

public class MvelScriptEngine extends AbstractScriptEngine implements ScriptEngine, Compilable {

    private volatile MvelScriptEngineFactory factory;

    private static String readFully(Reader reader) throws ScriptException {
        char[] arr = new char[8192];
        StringBuilder buf = new StringBuilder();

        int numChars;
        try {
            while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                buf.append(arr, 0, numChars);
            }
        } catch (IOException var5) {
            throw new ScriptException(var5);
        }

        return buf.toString();
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        Serializable expression = compiledScript(script);
        return evaluate(expression, context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return this.eval(readFully(reader), context);
    }

    @Override
    public Bindings createBindings() {
        return new MvelBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        if (this.factory == null) {
            synchronized (this) {
                if (this.factory == null) {
                    this.factory = new MvelScriptEngineFactory();
                }
            }
        }

        return this.factory;
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        return new MvelCompiledScript(this, compiledScript(script));
    }

    @Override
    public CompiledScript compile(Reader reader) throws ScriptException {
        return this.compile(readFully(reader));
    }

    public Serializable compiledScript(String script) throws ScriptException {
        try {
            Serializable expression = MVEL.compileExpression(script);
            return expression;
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public Object evaluate(Serializable expression, ScriptContext context) throws ScriptException {
        try {
            return MVEL.executeExpression(expression, context.getBindings(ScriptContext.ENGINE_SCOPE));
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }
}
