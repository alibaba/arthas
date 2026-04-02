package com.taobao.arthas.core.command.view;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Term 的结果视图解析器
 * 负责管理和注册各种命令结果视图，根据结果模型类型查找对应的视图
 *
 * @author gongdewei 2020/3/27
 */
public class ResultViewResolver {
    private static final Logger logger = LoggerFactory.getLogger(ResultViewResolver.class);

    // 结果模型类到视图的映射表
    // key: 结果模型的 Class 对象
    // value: 对应的结果视图实例
    private Map<Class, ResultView> resultViewMap = new ConcurrentHashMap<Class, ResultView>();

    /**
     * 构造函数
     * 在创建解析器时自动初始化并注册所有结果视图
     */
    public ResultViewResolver() {
        initResultViews();
    }

    /**
     * 初始化并注册所有的 ResultView
     * 需要调用此方法初始化注册ResultView
     * 在构造函数中自动调用，注册所有内置的命令结果视图
     */
    private void initResultViews() {
        try {
            // 注册基础视图
            registerView(RowAffectView.class);

            // 注册 basic1000 分组的视图
            // 这些是基础命令的视图，包括状态查询、版本信息、帮助等
            registerView(StatusView.class);         // 状态视图
            registerView(VersionView.class);        // 版本视图
            registerView(MessageView.class);        // 消息视图
            registerView(HelpView.class);           // 帮助视图
            //registerView(HistoryView.class);       // 历史视图（已注释）
            registerView(EchoView.class);           // 回显视图
            registerView(CatView.class);            // 文件查看视图
            registerView(Base64View.class);         // Base64 编解码视图
            registerView(OptionsView.class);        // 选项视图
            registerView(SystemPropertyView.class); // 系统属性视图
            registerView(SystemEnvView.class);      // 系统环境变量视图
            registerView(PwdView.class);            // 当前工作目录视图
            registerView(VMOptionView.class);       // JVM 选项视图
            registerView(SessionView.class);        // 会话视图
            registerView(ResetView.class);          // 重置视图
            registerView(ShutdownView.class);       // 关闭视图

            // 注册 klass100 分组的视图
            // 这些是类操作相关的命令视图
            registerView(ClassLoaderView.class);    // 类加载器视图
            registerView(DumpClassView.class);      // 类转储视图
            registerView(GetStaticView.class);      // 获取静态字段视图
            registerView(JadView.class);            // 反编译视图
            registerView(MemoryCompilerView.class); // 内存编译器视图
            registerView(OgnlView.class);           // OGNL 表达式视图
            registerView(RedefineView.class);       // 类重定义视图
            registerView(RetransformView.class);    // 类转换视图
            registerView(SearchClassView.class);    // 类搜索视图
            registerView(SearchMethodView.class);   // 方法搜索视图

            // 注册 logger 分组的视图
            registerView(LoggerView.class);         // 日志视图

            // 注册 monitor2000 分组的视图
            // 这些是监控和诊断相关的命令视图
            registerView(DashboardView.class);      // 仪表盘视图
            registerView(JvmView.class);            // JVM 信息视图
            registerView(MemoryView.class);         // 内存视图
            registerView(MBeanView.class);          // MBean 视图
            registerView(PerfCounterView.class);    // 性能计数器视图
            registerView(ThreadView.class);         // 线程视图
            registerView(ProfilerView.class);       // 性能分析视图
            registerView(EnhancerView.class);       // 增强视图
            registerView(MonitorView.class);        // 监控视图
            registerView(StackView.class);          // 栈跟踪视图
            registerView(TimeTunnelView.class);     // 时间隧道视图
            registerView(TraceView.class);          // 跟踪视图
            registerView(WatchView.class);          // 观察视图
            registerView(VmToolView.class);         // VM 工具视图
            registerView(JFRView.class);            // Java Flight Recorder 视图

        } catch (Throwable e) {
            logger.error("注册结果视图失败", e);
        }
    }

    /**
     * 根据结果模型获取对应的结果视图
     *
     * @param model 结果模型对象
     * @return 对应的结果视图，如果不存在则返回 null
     */
    public ResultView getResultView(ResultModel model) {
        return resultViewMap.get(model.getClass());
    }

    /**
     * 手动注册结果视图
     * 将指定的结果模型类和视图实例关联起来
     *
     * @param modelClass 结果模型的 Class 对象
     * @param view 结果视图实例
     * @return 当前解析器对象，支持链式调用
     */
    public ResultViewResolver registerView(Class modelClass, ResultView view) {
        //TODO 检查model的type是否重复，避免复制代码带来的bug
        this.resultViewMap.put(modelClass, view);
        return this;
    }

    /**
     * 注册结果视图
     * 通过反射自动获取视图对应的结果模型类
     *
     * @param view 结果视图实例
     * @return 当前解析器对象，支持链式调用
     * @throws NullPointerException 如果无法获取模型类
     */
    public ResultViewResolver registerView(ResultView view) {
        Class modelClass = getModelClass(view);
        if (modelClass == null) {
            throw new NullPointerException("结果模型类为空");
        }
        return this.registerView(modelClass, view);
    }

    /**
     * 通过反射创建并注册结果视图
     * 使用视图类的无参构造函数创建实例
     *
     * @param viewClass 结果视图的 Class 对象
     * @throws RuntimeException 如果创建实例失败
     */
    public void registerView(Class<? extends ResultView> viewClass) {
        ResultView view = null;
        try {
            // 使用反射创建视图实例
            view = viewClass.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("创建视图实例失败，视图类：" + viewClass, e);
        }
        this.registerView(view);
    }

    /**
     * 获取结果视图对应的结果模型类
     * 通过反射分析视图类的 draw 方法，提取第二个参数的类型（即 ResultModel 的具体类型）
     *
     * @param <V> 结果视图的类型
     * @param view 结果视图实例
     * @return 结果模型的 Class 对象，如果无法识别则返回 null
     */
    public static <V extends ResultView> Class getModelClass(V view) {
        //类反射获取子类的draw方法第二个参数的ResultModel具体类型
        Class<? extends ResultView> viewClass = view.getClass();
        // 获取类中声明的所有方法
        Method[] declaredMethods = viewClass.getDeclaredMethods();
        for (int i = 0; i < declaredMethods.length; i++) {
            Method method = declaredMethods[i];
            // 查找名为 "draw" 的方法
            if (method.getName().equals("draw")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                // 检查方法签名：
                // 1. 有两个参数
                // 2. 第一个参数是 CommandProcess 类型
                // 3. 第二个参数不是 ResultModel 本身（而是其子类）
                // 4. 第二个参数是 ResultModel 的子类
                if (parameterTypes.length == 2
                        && parameterTypes[0] == CommandProcess.class
                        && parameterTypes[1] != ResultModel.class
                        && ResultModel.class.isAssignableFrom(parameterTypes[1])) {
                    // 找到了，返回第二个参数的类型
                    return parameterTypes[1];
                }
            }
        }
        // 未找到匹配的方法
        return null;
    }
}
