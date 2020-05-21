package com.taobao.arthas.core.command.view;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gongdewei 2020/3/27
 */
public class ResultViewResolver {
    private static final Logger logger = LoggerFactory.getLogger(ResultViewResolver.class);

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
            registerView(new StatusModel(), new StatusView());
            registerView(new VersionModel(), new VersionView());
            registerView(new MessageModel(), new MessageView());
            registerView(new HelpModel(), new HelpView());
            //registerView(new HistoryModel(), new HistoryView());
            registerView(new EchoModel(), new EchoView());
        } catch (Throwable e) {
            logger.error("register result view failed", e);
        }
    }

    private ResultViewResolver() {
    }

//    public void registerView(Class<? extends ExecResult> resultClass, ResultView view) throws IllegalAccessException, InstantiationException {
//        ExecResult instance = resultClass.newInstance();
//        this.registerView(instance.getType(), view);
//    }

    public <T extends ResultModel> void registerView(T resultObject, ResultView view) {
        this.registerView(resultObject.getType(), view);
    }

    public void registerView(String resultType, ResultView view) {
        resultViewMap.put(resultType, view);
    }

    public ResultView getResultView(String resultType) {
        return resultViewMap.get(resultType);
    }
}
