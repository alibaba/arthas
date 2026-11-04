
package org.example.jfranalyzerbackend.util;



import org.example.jfranalyzerbackend.model.Frame;
import org.example.jfranalyzerbackend.model.JavaFrame;
import org.example.jfranalyzerbackend.model.JavaMethod;
import org.example.jfranalyzerbackend.model.StackTrace;
import org.example.jfranalyzerbackend.model.jfr.RecordedClass;
import org.example.jfranalyzerbackend.model.jfr.RecordedFrame;
import org.example.jfranalyzerbackend.model.jfr.RecordedMethod;
import org.example.jfranalyzerbackend.model.jfr.RecordedStackTrace;
import org.example.jfranalyzerbackend.model.symbol.SymbolBase;
import org.example.jfranalyzerbackend.model.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class StackTraceUtil {
    public static final RecordedStackTrace DUMMY_STACK_TRACE = StackTraceUtil.newDummyStackTrace("", "", "NO Frame");


    public static StackTrace build(RecordedStackTrace stackTrace, SymbolTable<SymbolBase> symbols) {
        StackTrace result = new StackTrace();
        result.setTruncated(stackTrace.isTruncated());

        List<RecordedFrame> srcFrames = stackTrace.getFrames();
        Frame[] dstFrames = new Frame[srcFrames.size()];
        for (int i = 0; i < srcFrames.size(); i++) {
            RecordedFrame frame = srcFrames.get(i);
            Frame dstFrame;
            if (frame.isJavaFrame()) {
                dstFrame = new JavaFrame();
                ((JavaFrame) dstFrame).setJavaFrame(frame.isJavaFrame());
                ((JavaFrame) dstFrame).setType(JavaFrame.Type.typeOf(frame.getType()));
                ((JavaFrame) dstFrame).setBci(frame.getBytecodeIndex());
            } else {
                dstFrame = new Frame();
            }

            RecordedMethod method = frame.getMethod();
            JavaMethod dstMethod = new JavaMethod();
            dstMethod.setPackageName(method.getType().getPackageName());
            dstMethod.setType(method.getType().getName());
            dstMethod.setName(method.getName());
            dstMethod.setDescriptor(method.getDescriptor()); // 直接使用原始描述符

            dstMethod.setModifiers(method.getModifiers());
            dstMethod.setHidden(method.isHidden());
            if (symbols.isContains(dstMethod)) {
                dstMethod = (JavaMethod) symbols.get(dstMethod);
            } else {
                symbols.put(dstMethod);
            }

            dstFrame.setMethod(dstMethod);
            dstFrame.setLine(frame.getLineNumber());
            if (symbols.isContains(dstFrame)) {
                dstFrame = (Frame) symbols.get(dstFrame);
            } else {
                symbols.put(dstFrame);
            }

            dstFrames[i] = dstFrame;
        }

        result.setFrames(dstFrames);
        if (symbols.isContains(result)) {
            result = (StackTrace) symbols.get(result);
        } else {
            symbols.put(result);
        }

        return result;
    }

    public static RecordedStackTrace newDummyStackTrace(String packageName, String className, String methodName) {
        RecordedStackTrace st = new RecordedStackTrace();
        List<RecordedFrame> list = new ArrayList<>();
        RecordedFrame f = new RecordedFrame();
        RecordedMethod m = new RecordedMethod();
        RecordedClass c = new RecordedClass();
        c.setPackageName(packageName);
        c.setName(className);
        m.setType(c);
        f.setMethod(m);
        m.setName(methodName);
        list.add(f);
        st.setFrames(list);
        return st;
    }
}
