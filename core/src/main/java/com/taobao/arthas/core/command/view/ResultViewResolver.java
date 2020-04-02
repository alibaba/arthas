package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.*;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gongdewei 2020/3/27
 */
public class ResultViewResolver {
    private static final Logger logger = LogUtil.getArthasLogger();

    private Map<String, ResultView> resultViewMap = new ConcurrentHashMap<String, ResultView>();

    private static ResultViewResolver viewResolver;

    public static ResultViewResolver getInstance() {
        if (viewResolver == null) {
            synchronized (ResultViewResolver.class) {
                viewResolver = new ResultViewResolver();
            }
        }
        return viewResolver;
    }

    static {
        getInstance().registerResultViews();
    }

    private void registerResultViews() {
        try {
            registerView(new SessionResult(), new SessionView());
            registerView(new StatusResult(), new StatusView());
            registerView(new WatchResult(), new WatchView());
            registerView(new EnhancerAffectResult(), new EnhancerAffectView());
            registerView(new VersionResult(), new VersionView());
            registerView(new PropertyResult(), new PropertyView());
            registerView(new MessageResult(), new MessageView());
        } catch (Throwable e) {
            logger.error("arthas", "register result view failed", e);
        }
    }

    private ResultViewResolver() {
    }

//    public void registerView(Class<? extends ExecResult> resultClass, ResultView view) throws IllegalAccessException, InstantiationException {
//        ExecResult instance = resultClass.newInstance();
//        this.registerView(instance.getType(), view);
//    }

    public <T extends ExecResult> void registerView(T resultObject, ResultView view) {
        this.registerView(resultObject.getType(), view);
    }

    public void registerView(String resultType, ResultView view) {
        resultViewMap.put(resultType, view);
    }

    public ResultView getResultView(String resultType) {
        return resultViewMap.get(resultType);
    }
}
