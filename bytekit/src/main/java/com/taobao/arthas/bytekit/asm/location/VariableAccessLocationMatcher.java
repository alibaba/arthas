package com.taobao.arthas.bytekit.asm.location;

import java.util.List;

import com.taobao.arthas.bytekit.asm.MethodProcessor;

public class VariableAccessLocationMatcher extends AccessLocationMatcher {

    /**
     * the name of the variable being accessed at the point where the trigger
     * point should be inserted
     */
    private String variableName;

    /**
     * flag which is true if the name is a method parameter index such as $0, $1
     * etc otherwise false
     */
    private boolean isIndex;


    protected VariableAccessLocationMatcher(String variablename, int count, int flags, boolean whenComplete) {
        super(count, flags, whenComplete);
        this.variableName = variablename;
        isIndex = variablename.matches("[0-9]+");
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        // TODO Auto-generated method stub
        return null;
    }

}
