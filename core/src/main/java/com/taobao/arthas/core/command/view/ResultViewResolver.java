package com.taobao.arthas.core.command.view;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Result view resolver for term
 *
 * @author gongdewei 2020/3/27
 */
public class ResultViewResolver {
    private static final Logger logger = LoggerFactory.getLogger(ResultViewResolver.class);

    static {
        getInstance().registerResultViews();
    }

    // modelClass -> view
    private Map<Class, ResultView> resultViewMap = new ConcurrentHashMap<Class, ResultView>();

    private static ResultViewResolver viewResolver;

    public static ResultViewResolver getInstance() {
        if (viewResolver == null) {
            synchronized (ResultViewResolver.class) {
                viewResolver = new ResultViewResolver();
            }
        }
        return viewResolver;
    }

    private void registerResultViews() {
        try {
            //basic1000
            registerView(StatusView.class);
            registerView(VersionView.class);
            registerView(MessageView.class);
            registerView(HelpView.class);
            //registerView(HistoryView.class);
            registerView(EchoView.class);
            registerView(CatView.class);
            registerView(EnhancerAffectView.class);
            registerView(OptionsView.class);
            registerView(SystemPropertyView.class);
            registerView(SystemEnvView.class);
            registerView(PwdView.class);
            registerView(VMOptionView.class);
            registerView(SessionView.class);

            //klass100

            //logger

            //monitor2000

        } catch (Throwable e) {
            logger.error("register result view failed", e);
        }
    }

    private ResultViewResolver() {
    }

    public ResultView getResultView(ResultModel model) {
        return resultViewMap.get(model.getClass());
    }

    public ResultViewResolver registerView(Class modelClass, ResultView view) {
        //TODO 检查model的type是否重复，避免复制代码带来的bug
        this.resultViewMap.put(modelClass, view);
        return this;
    }

    public ResultViewResolver registerView(ResultView view) {
        Class modelClass = getModelClass(view);
        if (modelClass == null) {
            throw new NullPointerException("model class is null");
        }
        return this.registerView(modelClass, view);
    }

    public void registerView(Class<? extends ResultView> viewClass) {
        ResultView view = null;
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
    public static <V extends ResultView> Class getModelClass(V view) {
        //类反射获取子类的draw方法第二个参数的ResultModel具体类型
        Class<? extends ResultView> viewClass = view.getClass();
        Method[] declaredMethods = viewClass.getDeclaredMethods();
        for (int i = 0; i < declaredMethods.length; i++) {
            Method method = declaredMethods[i];
            if (method.getName().equals("draw")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 2
                        && parameterTypes[0] == CommandProcess.class
                        && parameterTypes[1] != ResultModel.class
                        && ResultModel.class.isAssignableFrom(parameterTypes[1])) {
                    return parameterTypes[1];
                }
            }
        }
        return null;
    }
}
