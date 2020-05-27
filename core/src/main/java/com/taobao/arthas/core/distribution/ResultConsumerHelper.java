package com.taobao.arthas.core.distribution;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.model.CatModel;
import com.taobao.arthas.core.command.model.ClassSetVO;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.TraceModel;
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
 * @author gongdewei 2020/5/18
 */
public class ResultConsumerHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResultConsumerHelper.class);

    private static Map<String, List<Field>> modelFieldMap = new ConcurrentHashMap<String, List<Field>>();

    /**
     * 估算命令执行结果的item数量
     * 注意：此方法调用频繁，不能产生内存碎片
     * @param model
     * @return
     */
    public static int getItemCount(ResultModel model) {
        int count = processSpecialCommand(model);
        if (count > 0) {
            return count;
        }

        //TODO 抽取ItemSet/ItemGroup接口，解决ClassSetVO/mbean等分组的情况

        //缓存Field对象，避免产生内存碎片
        Class modelClass = model.getClass();
        List<Field> fields = modelFieldMap.get(modelClass.getName());
        if (fields == null) {
            fields = new ArrayList<Field>();
            modelFieldMap.put(modelClass.getName(), fields);
            Field[] declaredFields = modelClass.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                Class<?> fieldClass = field.getType();
                if (Collection.class.isAssignableFrom(fieldClass)
                        || Map.class.isAssignableFrom(fieldClass)
                        || fieldClass.isArray()
                        || fieldClass == ClassSetVO.class) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }

        //获取item数量
        try {
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object value = field.get(model);
                if (value != null) {
                    if (value instanceof  Collection) {
                        return ((Collection) value).size();
                    } else if (value.getClass().isArray()) {
                        return Array.getLength(value);
                    } else if (value instanceof Map) {
                        return ((Map) value).size();
                    } else if (value.getClass() == ClassSetVO.class) {
                        return ((ClassSetVO) value).getClasses().size();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("get item count of result model failed, model: {}", JSON.toJSONString(model), e);
        }

        return 1;
    }

    private static int processSpecialCommand(ResultModel model) {
        if (model instanceof CatModel) {
            //特殊处理cat
            return ((CatModel) model).getContent().length()/100 + 1 ;
        } else if (model instanceof TraceModel) {
            //特殊处理trace
            return ((TraceModel) model).getNodeCount();
        }
        return 0;
    }

}
