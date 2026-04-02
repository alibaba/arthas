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
 * 该类提供了对命令执行结果模型（ResultModel）的辅助操作，主要是估算结果中包含的数据项数量。
 * 这个数量值作为 Consumer 分发时进行切片的参考依据，避免单次发送大量数据导致性能问题或内存溢出。
 *
 * 主要功能：
 * 1. 估算 ResultModel 中包含的数据项数量
 * 2. 支持多种数据类型：Collection、Map、Array、Countable
 * 3. 使用缓存优化反射操作的性能，避免频繁反射导致的内存碎片
 *
 * @author gongdewei 2020/5/18
 */
public class ResultConsumerHelper {

    /**
     * 日志记录器，用于记录错误和调试信息
     */
    private static final Logger logger = LoggerFactory.getLogger(ResultConsumerHelper.class);

    /**
     * 模型字段缓存映射
     *
     * 该缓存用于存储每个 ResultModel 类中可统计数量的字段（如 List、Map、Array 等）。
     * 使用 ConcurrentHashMap 保证线程安全。
     *
     * Key: 类的全限定名
     * Value: 该类中可统计数量的字段列表
     *
     * 通过缓存 Field 对象避免频繁使用反射获取字段，减少内存碎片和性能开销。
     */
    private static ConcurrentHashMap<String, List<Field>> modelFieldMap = new ConcurrentHashMap<String, List<Field>>();

    /**
     * 估算命令执行结果的数据项数量
     *
     * 该方法通过分析 ResultModel 对象的内容，估算其中包含的数据项数量。
     * 这个数量值用作 Consumer 分发时进行切片的参考依据，避免单次发送大量数据。
     *
     * 计算策略：
     * 1. 如果 ResultModel 实现了 Countable 接口，直接调用其 size() 方法获取数量
     * 2. 否则，通过反射遍历 Model 的所有字段，统计其中的 Collection、Map、Array、Countable 类型字段的元素数量
     * 3. 将所有字段的元素数量累加得到总数量
     *
     * 性能优化：
     * - 使用 ConcurrentHashMap 缓存每个类的可统计字段，避免重复反射获取字段
     * - 缓存 Field 对象并设置为可访问，避免每次调用时的反射开销
     *
     * 注意事项：
     * - 此方法会被频繁调用，因此要特别注意避免产生内存碎片
     * - 如果无法计算或计算结果为 0，返回 1 作为默认值，确保至少有一个数据项
     *
     * @param model 需要估算数据项数量的命令结果模型对象，不能为 null
     * @return 估算的数据项数量，最小值为 1
     */
    public static int getItemCount(ResultModel model) {
        // 如果实现 Countable 接口，则认为 model 自己统计元素数量
        // 这是最准确和高效的方式，优先使用
        if (model instanceof Countable) {
            return ((Countable) model).size();
        }

        // 对于普通的 Model，通过类反射统计容器类字段的元素数量
        // 首先尝试从缓存中获取该类的可统计字段列表
        Class modelClass = model.getClass();
        List<Field> fields = modelFieldMap.get(modelClass.getName());
        if (fields == null) {
            // 缓存未命中，需要通过反射获取字段信息
            fields = new ArrayList<Field>();
            // 获取类中声明的所有字段（包括私有字段）
            Field[] declaredFields = modelClass.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                Class<?> fieldClass = field.getType();
                // 如果是 List/Map/Array/Countable 类型的字段，则缓存起来后面统计数量
                // 这些字段类型都包含可统计数量的元素
                if (Collection.class.isAssignableFrom(fieldClass)
                        || Map.class.isAssignableFrom(fieldClass)
                        || Countable.class.isAssignableFrom(fieldClass)
                        || fieldClass.isArray()) {
                    // 设置字段可访问，以便后续获取字段值
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            // 使用 putIfAbsent 保证线程安全，避免重复创建字段列表
            // 如果其他线程已经创建了该类的字段列表，则使用已存在的
            List<Field> old_fields = modelFieldMap.putIfAbsent(modelClass.getName(), fields);
            if (old_fields != null) {
                fields = old_fields;
            }
        }

        // 统计 Model 对象的 item 总数量
        int count = 0;
        try {
            // 遍历所有可统计的字段
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                // 确保字段可访问（可能被其他代码修改）
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                // 获取字段在当前 model 实例中的值
                Object value = field.get(model);
                if (value != null) {
                    // 根据字段值的类型，采用不同方式统计元素数量
                    if (value instanceof Collection) {
                        // Collection 类型（List、Set 等），直接调用 size() 方法
                        count += ((Collection) value).size();
                    } else if (value.getClass().isArray()) {
                        // 数组类型，使用 Array.getLength() 获取长度
                        count += Array.getLength(value);
                    } else if (value instanceof Map) {
                        // Map 类型，调用 size() 方法获取键值对数量
                        count += ((Map) value).size();
                    } else if (value instanceof Countable) {
                        // Countable 类型，调用其 size() 方法
                        count += ((Countable) value).size();
                    }
                }
            }
        } catch (Exception e) {
            // 发生异常时记录错误日志，包含 model 的详细信息便于调试
            logger.error("get item count of result model failed, model: {}", JSON.toJSONString(model), e);
        }

        // 如果统计结果为 0 或负数，返回 1 作为默认值
        // 这样确保至少有一个数据项，避免分片逻辑出现问题
        return count > 0 ? count : 1;
    }

}
