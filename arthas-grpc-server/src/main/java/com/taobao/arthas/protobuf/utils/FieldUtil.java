package com.taobao.arthas.protobuf.utils;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:33
 */

import com.baidu.bjf.remoting.protobuf.annotation.Ignore;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.taobao.arthas.protobuf.ProtobufField;
import com.taobao.arthas.protobuf.annotation.ProtobufCustomedField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:33
 * @description: FieldUtil
 */
public class FieldUtil {

    public static List<ProtobufField> getProtobufFieldList(Class<?> clazz, boolean enableCustomedField) {
        // 获取所有的 java field
        List<Field> fields = new ArrayList<>();
        Field[] fieldsArray = clazz.getFields();
        for (Field field : fieldsArray) {
            if (enableCustomedField) {
                ProtobufCustomedField annotation = field.getAnnotation(ProtobufCustomedField.class);
                if (annotation != null) {
                    fields.add(field);
                }
            } else {
                fields.add(field);
            }
        }

        // 转化为 protobuf field
        List<ProtobufField> protobufFields = new ArrayList<>();
        for (Field field : fields) {
            if (field.getAnnotation(Ignore.class) != null || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            String fieldName = field.getName();
            String filedTypeName = field.getType().getName();

            // protobuf 不支持除字节数组以外任何数组
            if (filedTypeName.startsWith("[")) {
                if ((!filedTypeName.equals(byte[].class.getName())) && (!filedTypeName.equals(Byte[].class.getName()))) {
                    throw new RuntimeException("Array type of field '" + fieldName + "' on class '"
                            + field.getDeclaringClass().getName() + "' is not support,  please use List instead.");
                }
            }


        }
        //TODO
        return null;
    }
}
