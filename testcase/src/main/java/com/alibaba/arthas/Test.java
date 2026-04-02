package com.alibaba.arthas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 测试类
 *
 * 这是一个用于Arthas测试用例的演示类。
 * 主要用于展示Arthas的各种监控和诊断功能，包括：
 * - 类静态属性监控
 * - 对象属性修改
 * - 方法调用追踪
 * - 线程状态监控
 *
 * 测试场景：
 * 1. 创建包含40个Pojo对象的列表
 * 2. 在循环中随机选择对象并修改其属性
 * 3. 调用测试方法并睡眠
 * 4. 便于使用Arthas命令观察运行时状态
 *
 * @author diecui1202 on 2017/9/13.
 */
public class Test {

    /**
     * 静态Map属性m
     *
     * 用于测试静态属性监控功能。
     * 在静态初始化块中初始化，包含两个键值对：
     * - "a" -> "aaa"
     * - "b" -> "bbb"
     */
    public static final Map m = new HashMap();

    /**
     * 静态Map属性n
     *
     * 用于测试静态属性监控功能，使用枚举类型作为键。
     * 在静态初始化块中初始化，包含两个键值对：
     * - Type.RUN -> "aaa"
     * - Type.STOP -> "bbb"
     */
    public static final Map n = new HashMap();

    /**
     * 静态Map属性p
     *
     * 用于测试null值的静态属性监控。
     * 该属性被显式初始化为null。
     */
    public static final Map p = null;

    /**
     * 静态初始化块
     *
     * 在类加载时执行，用于初始化静态属性m和n。
     * 向m中添加字符串键值对，向n中添加枚举键值对。
     */
    static {
        // 向Map m中添加两个字符串键值对
        m.put("a", "aaa");
        m.put("b", "bbb");

        // 向Map n中添加两个枚举类型键值对
        n.put(Type.RUN, "aaa");
        n.put(Type.STOP, "bbb");
    }

    /**
     * 主方法，程序入口
     *
     * 执行以下操作：
     * 1. 创建包含40个Pojo对象的列表
     * 2. 为每个Pojo对象设置name和age属性
     * 3. 打印null属性p的值（用于测试）
     * 4. 进入无限循环，随机修改Pojo对象的name属性
     * 5. 每次循环后睡眠1秒
     *
     * 此场景便于使用Arthas命令观察：
     * - 对象属性的变化
     * - 方法的调用情况
     * - 线程的运行状态
     * - 变量的值变化
     *
     * @param args 命令行参数（未使用）
     * @throws InterruptedException 当线程睡眠被中断时抛出
     */
    public static void main(String[] args) throws InterruptedException {
        // 创建用于存储Pojo对象的列表
        List<Pojo> list = new ArrayList<Pojo>();

        // 循环创建40个Pojo对象并添加到列表中
        for (int i = 0; i < 40; i ++) {
            // 创建新的Pojo对象
            Pojo pojo = new Pojo();
            // 设置name属性为"name "加上索引值
            pojo.setName("name " + i);
            // 设置age属性为索引值加2
            pojo.setAge(i + 2);

            // 将配置好的Pojo对象添加到列表
            list.add(pojo);
        }

        // 打印null属性的值，用于测试null值的处理
        System.out.println(p);

        // 进入无限循环，持续运行以便于观察
        while (true) {
            // 生成0-39之间的随机索引
            int random = new Random().nextInt(40);
            // 获取随机位置的Pojo对象的name属性
            String name = list.get(random).getName();
            // 将随机位置的Pojo对象的name属性临时设置为null
            list.get(random).setName(null);
            // 调用test方法，用于测试方法调用追踪
            test(list);
            // 恢复Pojo对象的原始name属性
            list.get(random).setName(name);
            // 线程睡眠1秒，模拟实际业务场景的间隔
            Thread.sleep(1000L);
        }
    }

    /**
     * 测试方法
     *
     * 这是一个空方法，用于测试Arthas的方法调用追踪功能。
     * 在main方法中被调用，但不执行任何实际操作。
     *
     * 使用场景：
     * - 使用Arthas的watch命令观察方法调用
     * - 使用Arthas的trace命令追踪方法调用链
     * - 使用Arthas的monitor命令监控方法调用统计
     *
     * @param list Pojo对象列表，虽然接收参数但未使用
     */
    public static void test(List<Pojo> list) {
        // do nothing
        // 空方法体，不执行任何操作
        // 仅用于演示方法调用追踪功能
    }

    /**
     * 调用方法
     *
     * 这是一个简单的测试方法，用于演示方法调用和参数观察。
     * 该方法接收一个字符串参数并打印到控制台。
     *
     * 使用场景：
     * - 演示Arthas如何观察方法参数
     * - 演示Arthas如何观察方法返回值
     * - 演示Arthas如何修改方法参数
     *
     * @param a 要打印的字符串参数
     */
    public static void invoke(String a) {
        // 将参数打印到控制台
        System.out.println(a);
    }
}
