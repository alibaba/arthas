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
            registerView(new SessionModel(), new SessionView());
            registerView(new StatusModel(), new StatusView());
            registerView(new WatchModel(), new WatchView());
            registerView(new EnhancerAffectModel(), new EnhancerAffectView());
            registerView(new VersionModel(), new VersionView());
            registerView(new PropertyModel(), new PropertyView());
            registerView(new MessageModel(), new MessageView());
            registerView(new HelpListModel(), new HelpListView());
            registerView(new HelpDetailModel(), new HelpDetailView());
            //registerView(new HistoryModel(), new HistoryView());
            registerView(new ClassInfoModel(), new ClassInfoView());
            registerView(new RowAffectModel(), new RowAffectView());
            registerView(new MethodModel(), new MethodView());
            registerView(new StackModel(), new StackView());
            registerView(new VMOptionModel(), new VMOptionView());
            registerView(new OptionsModel(), new OptionsView());
            registerView(new ChangeResultModel(), new ChangeResultView());
            registerView(new RedefineModel(), new RedefineView());
            registerView(new MemoryCompilerModel(), new MemoryCompilerView());
            registerView(new ClassMatchesModel(), new ClassMatchesView());
            registerView(new GetStaticModel(), new GetStaticView());
            registerView(new DumpClassModel(), new DumpClassView());
            registerView(new ClassLoaderModel(), new ClassLoaderView());
            registerView(new JadModel(), new JadView());
            registerView(new LoggerModel(), new LoggerView());
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
