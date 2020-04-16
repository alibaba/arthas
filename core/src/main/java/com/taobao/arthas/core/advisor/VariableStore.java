package com.taobao.arthas.core.advisor;

public interface VariableStore {

    void variableStored(int line,String varName,Object varValue);
}
