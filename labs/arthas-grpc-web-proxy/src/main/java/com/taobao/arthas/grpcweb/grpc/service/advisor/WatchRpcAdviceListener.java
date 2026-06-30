package com.taobao.arthas.grpcweb.grpc.service.advisor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.arthas.grpcweb.grpc.model.WatchRequestModel;
import com.taobao.arthas.grpcweb.grpc.model.WatchResponseModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WatchRpcAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WatchRpcAdviceListener.class);
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    private final AtomicInteger idGenerator = new AtomicInteger(0);

    private Map<Long/*RESULT_ID*/, Object> results = new HashMap<>();
    private WatchRequestModel watchRequestModel;

    private ArthasStreamObserver arthasStreamObserver;

    public WatchRpcAdviceListener(ArthasStreamObserver arthasStreamObserver, boolean verbose) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.watchRequestModel = (WatchRequestModel) arthasStreamObserver.getRequestModel();
        super.setVerbose(verbose);
    }

    public void setArthasStreamObserver(ArthasStreamObserver arthasStreamObserver) {
        this.arthasStreamObserver = arthasStreamObserver;
        this.watchRequestModel = (WatchRequestModel) arthasStreamObserver.getRequestModel();
    }

    private boolean isFinish() {
        return watchRequestModel.isFinish() || !watchRequestModel.isBefore() && !watchRequestModel.isException() && !watchRequestModel.isSuccess();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
        if (watchRequestModel.isBefore()) {
            watching(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        if (watchRequestModel.isSuccess()) {
            watching(advice);
        }
        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (watchRequestModel.isException()) {
            watching(advice);
        }
        finishing(advice);
    }

    private void finishing(Advice advice) {
        if (isFinish()) {
            watching(advice);
        }
    }

    private void watching(Advice advice) {
        try {
            // 本次调用的耗时
            System.out.println("************job:  "+ arthasStreamObserver.getJobId() + "  rpc watch advice开始正式执行,执行信息如下*****************");
            System.out.println("listener ID: + " + arthasStreamObserver.getListener().id());
            System.out.println("参数: \n" + watchRequestModel.toString());
            System.out.println("###################***************** \n\n");
            double cost = threadLocalWatch.costInMillis();
            boolean conditionResult = isConditionMet(watchRequestModel.getConditionExpress(), advice, cost);
            if (this.isVerbose()) {
                String msg = "Condition express: " + watchRequestModel.getConditionExpress() + " , result: " + conditionResult + "\n";
                arthasStreamObserver.appendResult(new MessageModel(msg));
            }
            if (conditionResult) {
                long resultId = idGenerator.incrementAndGet();
                results.put(resultId, advice);
                Object value = getExpressionResult(watchRequestModel.getExpress(), advice, cost);

                WatchResponseModel model = new WatchResponseModel();
                model.setResultId(resultId);
                model.setTs(LocalDateTime.now());
                model.setCost(cost);
                model.setValue(new ObjectVO(value, watchRequestModel.getExpand()));
                model.setSizeLimit(watchRequestModel.getSizeLimit());
                model.setClassName(advice.getClazz().getName());
                model.setMethodName(advice.getMethod().getName());
                if (advice.isBefore()) {
                    model.setAccessPoint(AccessPoint.ACCESS_BEFORE.getKey());
                } else if (advice.isAfterReturning()) {
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_RETUNING.getKey());
                } else if (advice.isAfterThrowing()) {
                    model.setAccessPoint(AccessPoint.ACCESS_AFTER_THROWING.getKey());
                }
                arthasStreamObserver.appendResult(model);
                arthasStreamObserver.times().incrementAndGet();
                if (isLimitExceeded(watchRequestModel.getNumberOfLimit(), arthasStreamObserver.times().get())) {
                    String msg = "Command execution times exceed limit: " + watchRequestModel.getNumberOfLimit()
                            + ", so command will exit.\n";
                    arthasStreamObserver.end();
                }
            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            arthasStreamObserver.end(-1, "watch failed, condition is: " + watchRequestModel.getConditionExpress() + ", express is: "
                    + watchRequestModel.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }
}
