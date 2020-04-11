package com.taobao.arthas.bytekit.asm.inst;


/**
 *
 * <pre>
 * 实现这个 invokeOrigin()，需要多步处理：
 *
 * 传入要被替换的类，读取到标记了 @Instrument 的类。 类名不一样的话，先替换类名？
 *
 * 然后查找所有的 field，如果有标记了 @NewField ，则增加到要被替换的类里。
 *
 * 然后查找所有的函数， 再查找是否在 旧类里有同样签名的，如果有，则执行清除行号， 替换 invokeOrigin() ，再 inline 原来的旧函数
 *
 * 再替换函数到 旧类里。
 *
 * 类名有可能要替换
 *
 * </pre>
 *
 *
 *
 *
 *
 * @author hengyunabc 2019-02-25
 *
 */
public class InstrumentApi {
    public static final <T> T invokeOrigin() {
        return null;
    }
}
