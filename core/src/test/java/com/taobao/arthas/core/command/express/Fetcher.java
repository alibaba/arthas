package com.taobao.arthas.core.command.express;


import java.util.ArrayList;
import java.util.List;

public class Fetcher {
    private final List<Fetch> completedFetches = new ArrayList<>();

    public boolean hasCompletedFetches() {
        return !completedFetches.isEmpty();
    }

    public boolean getCompletedFetches() {
        return hasCompletedFetches();
    }

    public Fetcher add(Fetch fetch) {
        completedFetches.add(fetch);
        return this;
    }

    public static class Fetch {
        private final List<FlowContext> flowContexts = new ArrayList<>();
        public boolean hasFlowContexts() {
            return !flowContexts.isEmpty();
        }

        public boolean getFlowContexts() {
            return !flowContexts.isEmpty();
        }

        public List<FlowContext> getFlowContexts1() {
            return flowContexts;
        }

        public Fetch add(FlowContext flowContext) {
            flowContexts.add(flowContext);
            return this;
        }

    }
}


