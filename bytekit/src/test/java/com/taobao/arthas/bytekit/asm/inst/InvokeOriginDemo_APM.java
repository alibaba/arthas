package com.taobao.arthas.bytekit.asm.inst;

/**
 *
 * @author hengyunabc 2019-03-18
 *
 */
public class InvokeOriginDemo_APM {

    public void returnVoid() {
        Object o = InstrumentApi.invokeOrigin();
        System.out.println(o);
    }

    public Void returnVoidObject() {
        Void v = InstrumentApi.invokeOrigin();
        System.out.println(v);
        return v;
    }

    public int returnInt(int i) {
        System.out.println("before");
        int value = InstrumentApi.invokeOrigin();
        System.out.println("after");
        return value + 123;
    }

    public int returnIntToObject(int i) {
        Object value = InstrumentApi.invokeOrigin();
        return 9998 + (Integer) value;
    }

    public int returnIntToInteger(int i) {

        Integer ixx = InstrumentApi.invokeOrigin();

        return ixx + 9998;
    }

    public static int returnIntStatic(int i) {
        int result = InstrumentApi.invokeOrigin();
        return 9998 + result;
    }

    public long returnLong() {
        long result = InstrumentApi.invokeOrigin();
        return 9998L + result;
    }

    public long returnLongToObject() {
        Long lll = InstrumentApi.invokeOrigin();
        return 9998L + lll;
    }

    public String[] returnStrArray() {
        String[] result = InstrumentApi.invokeOrigin();
        System.err.println(result);
        return result;
    }

    public String[] returnStrArrayWithArgs(int i, String s, long l) {
        System.out.println(i);
        String[] result = InstrumentApi.invokeOrigin();
        result[0] = "fff";
        return result;
    }

    public String returnStr() {
        System.err.println("ssss");
        Object result = InstrumentApi.invokeOrigin();
        return "hello" + result;
    }

    public Object returnObject() {
        InstrumentApi.invokeOrigin();
        return InvokeOriginDemo.class;
    }

    public int recursive(int i) {
        int result = InstrumentApi.invokeOrigin();

        System.err.println(result);
        return result;
    }
}
