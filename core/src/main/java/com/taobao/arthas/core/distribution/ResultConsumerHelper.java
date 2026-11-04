package com.taobao.arthas.core.distribution;

import com.alibaba.fastjson2.JSON;
import com.taobao.arthas.core.command.model.Countable;
import com.taobao.arthas.core.command.model.ResultModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令结果模型辅助类
 *
 * @author gongdewei 2020/5/18
 */
public class ResultConsumerHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResultConsumerHelper.class);

    private static ConcurrentHashMap<String, List<Field>> modelFieldMap = new ConcurrentHashMap<String, List<Field>>();

    /**
     * 估算命令执行结果的item数量，目的是提供一个度量值，作为Consumer分发时进行切片的参考依据，避免单次发送大量数据。
     * 注意：此方法调用频繁，避免产生内存碎片
     *
     * @param model
     * @return
     */
    public static int getItemCount(ResultModel model) {
        //如果实现Countable接口，则认为model自己统计元素数量
        if (model instanceof Countable) {
            return ((Countable) model).size();
        }

        //对于普通的Model，通过类反射统计容器类字段统计元素数量
        //缓存Field对象，避免产生内存碎片
        Class modelClass = model.getClass();
        List<Field> fields = modelFieldMap.get(modelClass.getName());
        if (fields == null) {
            fields = new ArrayList<Field>();
            Field[] declaredFields = modelClass.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                Class<?> fieldClass = field.getType();
                //如果是List/Map/Array/Countable类型的字段，则缓存起来后面统计数量
                if (Collection.class.isAssignableFrom(fieldClass)
                        || Map.class.isAssignableFrom(fieldClass)
                        || Countable.class.isAssignableFrom(fieldClass)
                        || fieldClass.isArray()) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            List<Field> old_fields = modelFieldMap.putIfAbsent(modelClass.getName(), fields);
            if (old_fields != null) {
                fields = old_fields;
            }
        }

        //统计Model对象的item数量
        int count = 0;
        try {
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object value = field.get(model);
                if (value != null) {
                    if (value instanceof Collection) {
                        count += ((Collection) value).size();
                    } else if (value.getClass().isArray()) {
                        count += Array.getLength(value);
                    } else if (value instanceof Map) {
                        count += ((Map) value).size();
                    } else if (value instanceof Countable) {
                        count += ((Countable) value).size();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("get item count of result model failed, model: {}", JSON.toJSONString(model), e);
        }

        return count > 0 ? count : 1;
    }

}
