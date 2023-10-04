package com.taobao.arthas.grpcweb.grpc.view;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Result view resolver for term
 *
 * @author xuyang 2023/8/15
 */
public class GrpcResultViewResolver {
    private static final Logger logger = LoggerFactory.getLogger(GrpcResultViewResolver.class);

    // modelClass -> view
    private Map<Class, GrpcResultView> resultViewMap = new ConcurrentHashMap<Class, GrpcResultView>();

    public GrpcResultViewResolver() {
        initResultViews();
    }

    /**
     * 需要调用此方法初始化注册ResultView
     */
    private void initResultViews() {
        try {
//            registerView(RowAffectView.class);

            //basic1000
            registerView(GrpcStatusView.class);
//            registerView(VersionView.class);
            registerView(GrpcMessageView.class);
//            registerView(HelpView.class);
            //registerView(HistoryView.class);
//            registerView(EchoView.class);
//            registerView(CatView.class);
//            registerView(Base64View.class);
//            registerView(OptionsView.class);
            registerView(GrpcSystemPropertyView.class);
//            registerView(SystemEnvView.class);
            registerView(GrpcPwdView.class);
//            registerView(VMOptionView.class);
//            registerView(SessionView.class);
//            registerView(ResetView.class);
//            registerView(ShutdownView.class);

            //klass100
//            registerView(ClassLoaderView.class);
//            registerView(DumpClassView.class);
//            registerView(GetStaticView.class);
//            registerView(JadView.class);
//            registerView(MemoryCompilerView.class);
//            registerView(OgnlView.class);
//            registerView(RedefineView.class);
//            registerView(RetransformView.class);
//            registerView(SearchClassView.class);
//            registerView(SearchMethodView.class);

            //logger
//            registerView(LoggerView.class);

            //monitor2000
//            registerView(DashboardView.class);
//            registerView(JvmView.class);
//            registerView(MemoryView.class);
//            registerView(MBeanView.class);
//            registerView(PerfCounterView.class);
//            registerView(ThreadView.class);
//            registerView(ProfilerView.class);
            registerView(GrpcEnhancerView.class);
//            registerView(MonitorView.class);
//            registerView(StackView.class);
//            registerView(TimeTunnelView.class);
//            registerView(TraceView.class);
            registerView(GrpcWatchView.class);
//            registerView(VmToolView.class);
//            registerView(JFRView.class);

        } catch (Throwable e) {
            logger.error("register result view failed", e);
        }
    }

    public GrpcResultView getResultView(ResultModel model) {
        return resultViewMap.get(model.getClass());
    }

    public GrpcResultViewResolver registerView(Class modelClass, GrpcResultView view) {
        //TODO 检查model的type是否重复，避免复制代码带来的bug
        this.resultViewMap.put(modelClass, view);
        return this;
    }

    public GrpcResultViewResolver registerView(GrpcResultView view) {
        Class modelClass = getModelClass(view);
        if (modelClass == null) {
            throw new NullPointerException("model class is null");
        }
        return this.registerView(modelClass, view);
    }

    public void registerView(Class<? extends GrpcResultView> viewClass) {
        GrpcResultView view = null;
        try {
            view = viewClass.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("create view instance failure, viewClass:" + viewClass, e);
        }
        this.registerView(view);
    }

    /**
     * Get model class of result view
     *
     * @return
     */
    public static <V extends GrpcResultView> Class getModelClass(V view) {
        //类反射获取子类的draw方法第二个参数的ResultModel具体类型
        Class<? extends GrpcResultView> viewClass = view.getClass();
        Method[] declaredMethods = viewClass.getDeclaredMethods();
        for (int i = 0; i < declaredMethods.length; i++) {
            Method method = declaredMethods[i];
            if (method.getName().equals("draw")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 2
                        && parameterTypes[0] == ArthasStreamObserver.class
                        && parameterTypes[1] != ResultModel.class
                        && ResultModel.class.isAssignableFrom(parameterTypes[1])) {
                    return parameterTypes[1];
                }
            }
        }
        return null;
    }
}
