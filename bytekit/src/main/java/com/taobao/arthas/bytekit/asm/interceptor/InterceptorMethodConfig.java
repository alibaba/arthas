package com.taobao.arthas.bytekit.asm.interceptor;

import java.util.List;

import com.taobao.arthas.bytekit.asm.binding.Binding;

public class InterceptorMethodConfig {

    private boolean inline;

    private String owner;

    private String methodName;

    private String methodDesc;

    private List<Binding> bindings;

    /**
     * 插入的代码用 try/catch 包围的异常类型
     */
    private String suppress;

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public List<Binding> getBindings() {
        return bindings;
    }

    public void setBindings(List<Binding> bindings) {
        this.bindings = bindings;
    }

    public String getSuppress() {
        return suppress;
    }

    public void setSuppress(String suppress) {
        this.suppress = suppress;
    }
}
