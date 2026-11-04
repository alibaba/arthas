package com.taobao.arthas.core.command.express;

public class FlowContext {
    private FlowAttribute flowAttribute = new FlowAttribute();


    public FlowContext() {
    }

    public FlowContext(String app) {
        this.flowAttribute = new FlowAttribute(app);
    }

    public FlowAttribute getFlowAttribute() {
         return this.flowAttribute ;
    }
}
