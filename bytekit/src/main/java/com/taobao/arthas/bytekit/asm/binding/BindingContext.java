package com.taobao.arthas.bytekit.asm.binding;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.location.Location;

public class BindingContext {
    private MethodProcessor methodProcessor;
    private Location location;
    private StackSaver stackSaver;

    public BindingContext(Location location, MethodProcessor methodProcessor, StackSaver stackSaver) {
        this.location = location;
        this.methodProcessor = methodProcessor;
        this.stackSaver = stackSaver;
    }

    public MethodProcessor getMethodProcessor() {
        return methodProcessor;
    }

    public void setMethodProcessor(MethodProcessor methodProcessor) {
        this.methodProcessor = methodProcessor;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public StackSaver getStackSaver() {
        return stackSaver;
    }

    public void setStackSaver(StackSaver stackSaver) {
        this.stackSaver = stackSaver;
    }

}
