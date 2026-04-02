package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.affect.EnhancerAffect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 增强器模型工厂类
 *
 * <p>该工厂类负责从EnhancerAffect对象创建EnhancerModel和EnhancerAffectVO实例。
 * 基础的EnhancerModel和EnhancerAffectVO类定义在arthas-model模块中。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>将EnhancerAffect对象转换为EnhancerAffectVO视图对象</li>
 *   <li>创建带有增强效果的EnhancerModel实例</li>
 *   <li>支持全局配置选项（如dump和verbose模式）</li>
 *   <li>为其他类（如ResetModel）提供可复用的转换方法</li>
 * </ul>
 *
 * @author gongdewei 2020/7/20
 */
public class EnhancerModelFactory {

    /**
     * 创建增强器模型实例（无附加消息）
     *
     * @param affect 增强器影响对象，包含增强操作的统计信息
     * @param success 增强操作是否成功
     * @return 创建好的EnhancerModel实例
     */
    public static EnhancerModel create(EnhancerAffect affect, boolean success) {
        return new EnhancerModel(createEnhancerAffectVO(affect), success);
    }

    /**
     * 创建增强器模型实例（带附加消息）
     *
     * @param affect 增强器影响对象，包含增强操作的统计信息
     * @param success 增强操作是否成功
     * @param message 附加的消息文本，用于描述操作结果或提供额外信息
     * @return 创建好的EnhancerModel实例
     */
    public static EnhancerModel create(EnhancerAffect affect, boolean success, String message) {
        return new EnhancerModel(createEnhancerAffectVO(affect), success, message);
    }

    /**
     * 从EnhancerAffect对象创建EnhancerAffectVO视图对象
     *
     * <p>该方法为public访问级别，因此可以被其他类（如ResetModel）使用。
     * 转换过程中会根据全局配置选项决定是否包含详细信息：</p>
     * <ul>
     *   <li>如果启用了dump选项（GlobalOptions.isDump），会包含类dump文件的路径列表</li>
     *   <li>如果启用了verbose选项（GlobalOptions.verbose），会包含增强的方法列表</li>
     * </ul>
     *
     * @param affect 增强器影响对象，包含增强操作的统计信息。如果为null，返回一个默认的空VO对象
     * @return 转换后的EnhancerAffectVO视图对象，包含增强操作的详细信息
     */
    public static EnhancerAffectVO createEnhancerAffectVO(EnhancerAffect affect) {
        // 如果影响对象为null，返回一个默认的空VO对象
        // 使用-1表示无效的成本和监听器ID，0表示零计数
        if (affect == null) {
            return new EnhancerAffectVO(-1, 0, 0, -1);
        }

        // 创建基础VO对象，复制基本的统计信息
        // cost: 操作耗时（毫秒）
        // mCnt: 方法计数（增强的方法数量）
        // cCnt: 类计数（增强的类数量）
        // listenerId: 监听器ID
        EnhancerAffectVO vo = new EnhancerAffectVO(
            affect.cost(),
            affect.mCnt(),
            affect.cCnt(),
            affect.getListenerId()
        );

        // 设置可能发生的异常信息
        vo.setThrowable(affect.getThrowable());

        // 设置超限消息（当增强数量超过限制时的提示信息）
        vo.setOverLimitMsg(affect.getOverLimitMsg());

        // 如果全局启用了dump选项，则处理类dump文件
        if (GlobalOptions.isDump) {
            // 创建字符串列表来存储dump文件的绝对路径
            List<String> classDumpFiles = new ArrayList<String>();

            // 遍历所有dump文件，将它们的绝对路径添加到列表中
            for (File classDumpFile : affect.getClassDumpFiles()) {
                classDumpFiles.add(classDumpFile.getAbsolutePath());
            }

            // 将dump文件路径列表设置到VO对象中
            vo.setClassDumpFiles(classDumpFiles);
        }

        // 如果全局启用了verbose选项，则包含增强的方法列表
        if (GlobalOptions.verbose) {
            // 创建字符串列表来存储方法名称
            List<String> methods = new ArrayList<String>();

            // 将所有增强的方法名称添加到列表中
            methods.addAll(affect.getMethods());

            // 将方法列表设置到VO对象中
            vo.setMethods(methods);
        }

        // 返回构建完成的VO对象
        return vo;
    }
}
