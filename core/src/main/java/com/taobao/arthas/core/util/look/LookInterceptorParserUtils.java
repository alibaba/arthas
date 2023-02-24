package com.taobao.arthas.core.util.look;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.binding.StringBinding;
import com.alibaba.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.BindingParserUtils;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.bytekit.asm.location.LocationType;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.bytekit.utils.ReflectionUtils;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.taobao.arthas.core.advisor.SpyInterceptors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class LookInterceptorParserUtils {

    public static InterceptorProcessor createLookInterceptorProcessor(String lookLocation) {
        Method method = ReflectionUtils.findMethod(SpyInterceptors.SpyLookInterceptor.class, "atLookLocationLine", null);
        if (LookUtils.isLocationCode(lookLocation)) {
            method = ReflectionUtils.findMethod(SpyInterceptors.SpyLookInterceptor.class, "atLookLocationCode", null);
        }

        InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());

        //locationMatcher
        if (LookUtils.isLocationCode(lookLocation)) {
            interceptorProcessor.setLocationMatcher(new LookLocationCodeMatcher(lookLocation));
        } else {
            interceptorProcessor.setLocationMatcher(new LookLineNumberMatcher(lookLocation));
        }

        //interceptorMethodConfig
        InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
        interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);
        interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
        interceptorMethodConfig.setMethodName(method.getName());
        interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

        //inline
        interceptorMethodConfig.setInline(true);

        //bindings
        List<Binding> bindings = BindingParserUtils.parseBindings(method);
        for (Binding binding : bindings) {
            //因为注解值不能动态变化，所以需要在这里进行重新赋值，其实在这里生成binding也可以，但是方法注解会比较方便
            if (binding instanceof StringBinding) {
                StringBinding stringBinding = (StringBinding) binding;
                if (stringBinding.getValue().equals("LocationPlaceholder")) {
                    stringBinding.setValue(lookLocation);
                }
            }
        }
        interceptorMethodConfig.setBindings(bindings);

        return interceptorProcessor;
    }

    /**
     * 定义一个LookLocation
     * 为什么？因为其它已经定义的好的Location不太适用
     */
    public static class LookLocation extends Location {

        private String location;

        public LookLocation(AbstractInsnNode insnNode, String location, boolean whenComplete, boolean filtered) {
            super(insnNode, whenComplete, filtered);
            this.location = location;
        }

        @Override
        public LocationType getLocationType() {
            return LocationType.USER_DEFINE;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

    }


    /**
     * 行号匹配
     */
    public static class LookLineNumberMatcher implements LocationMatcher {

        /**
         * 目标行号
         * -1则代表方法的退出之前
         */
        private Integer targetLine;

        public LookLineNumberMatcher(String locationCode) {
            this.targetLine = Integer.valueOf(locationCode);
        }

        @Override
        public List<Location> match(MethodProcessor methodProcessor) {
            List<Location> locations = new ArrayList<Location>();
            AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();
            LocationFilter locationFilter = methodProcessor.getLocationFilter();

            while (insnNode != null) {

                if (targetLine == -1) {
                    //匹配方法退出之前，可能会有多个return语句
                    if (insnNode instanceof InsnNode) {
                        InsnNode node = (InsnNode) insnNode;
                        if (matchExit(node)) {
                            //行前匹配
                            boolean filtered = !locationFilter.allow(node, LocationType.LINE, false);
                            Location location = new LookLocation(node, targetLine.toString(), false, filtered);
                            locations.add(location);
                        }
                    }
                } else {
                    //匹配具体的行
                    if (insnNode instanceof LineNumberNode) {
                        LineNumberNode lineNumberNode = (LineNumberNode) insnNode;
                        if (matchLine(lineNumberNode.line)) {
                            //行前匹配
                            boolean filtered = !locationFilter.allow(lineNumberNode, LocationType.LINE, false);
                            //目前因为如果直接返回lineNumberNode，增强完之后会导致行号丢失，暂时没找到原因，因此取上一个节点
                            Location location = new LookLocation(lineNumberNode.getPrevious(), targetLine.toString(), false, filtered);
                            locations.add(location);
                            //存在一个方法内会有多个相同行号的情况，这里只取第一个
                            break;
                        }
                    }
                }
                insnNode = insnNode.getNext();
            }
            return locations;
        }

        private boolean matchLine(int line) {
            return line == targetLine;
        }

        public boolean matchExit(InsnNode node) {
            switch (node.getOpcode()) {
                case Opcodes.RETURN: // empty stack
                case Opcodes.IRETURN: // 1 before n/a after
                case Opcodes.FRETURN: // 1 before n/a after
                case Opcodes.ARETURN: // 1 before n/a after
                case Opcodes.LRETURN: // 2 before n/a after
                case Opcodes.DRETURN: // 2 before n/a after
                    return true;
            }
            return false;
        }

    }


    /**
     * 位置代码匹配
     */
    public static class LookLocationCodeMatcher implements LocationMatcher {

        private String locationCode = "";

        public LookLocationCodeMatcher(String locationCode) {
            this.locationCode = locationCode;
        }

        @Override
        public List<Location> match(MethodProcessor methodProcessor) {
            List<Location> locations = new ArrayList<Location>();
            LocationFilter locationFilter = methodProcessor.getLocationFilter();

            AbstractInsnNode insnNode = LookUtils.findInsnNodeByLocationCode(methodProcessor.getMethodNode(), locationCode);
            if (insnNode == null) {
                throw new IllegalStateException("invalid locationCode:" + locationCode);
            }
            boolean filtered = !locationFilter.allow(insnNode, LocationType.USER_DEFINE, false);
            Location location = new LookLocation(insnNode, locationCode, false, filtered);
            locations.add(location);
            return locations;
        }

    }

}
