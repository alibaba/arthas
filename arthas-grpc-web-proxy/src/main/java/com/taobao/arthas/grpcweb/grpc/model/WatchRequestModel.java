package com.taobao.arthas.grpcweb.grpc.model;

import io.arthas.api.ArthasServices.WatchRequest;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.service.advisor.WatchRpcAdviceListener;


public class WatchRequestModel extends EnhancerRequestModel {
    private String classPattern;
    private String methodPattern;
    private String express;
    private String conditionExpress;
    private boolean isBefore = false;
    private boolean isFinish = false;
    private boolean isException = false;
    private boolean isSuccess = false;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;
    private static final int MAX_EXPAND = 4;


    public String toString() {
        return "WatchRequestModel{" +
                "classPattern='" + classPattern + '\'' +
                ", methodPattern='" + methodPattern + '\'' +
                ", express='" + express + '\'' +
                ", conditionExpress='" + conditionExpress + '\'' +
                ", isBefore=" + isBefore +
                ", isFinish=" + isFinish +
                ", isException=" + isException +
                ", isSuccess=" + isSuccess +
                ", expand=" + expand +
                ", sizeLimit=" + sizeLimit +
                ", isRegEx=" + isRegEx +
                ", numberOfLimit=" + numberOfLimit +
                ", excludeClassPattern='" + excludeClassPattern + '\'' +
                ", jobId=" + jobId +
                ", listenerId=" + listenerId +
                ", verbose=" + verbose +
                ", maxNumOfMatchedClass=" + maxNumOfMatchedClass +
                '}';
    }

    public WatchRequestModel(WatchRequest watchRequest) {
        parseRequestParams(watchRequest);
    }

    public Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    public Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(ArthasStreamObserver arthasStreamObserver) {
        WatchRequestModel watchRequestModel = (WatchRequestModel) arthasStreamObserver.getRequestModel();
        if (watchRequestModel.getListenerId()!= 0) {
            AdviceListener listener = AdviceWeaver.listener(watchRequestModel.getListenerId());
            if (listener != null) {
                return listener;
            }
        }
        return new WatchRpcAdviceListener(arthasStreamObserver, GlobalOptions.verbose || watchRequestModel.isVerbose());
    }


    public Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    public void parseRequestParams(WatchRequest watchRequest){
        this.classPattern = watchRequest.getClassPattern();
        this.methodPattern = watchRequest.getMethodPattern();
        if(StringUtils.isEmpty(watchRequest.getExpress())){
            this.express = "{params, target, returnObj}";
        }else {
            this.express = watchRequest.getExpress();
        }
        this.conditionExpress = watchRequest.getConditionExpress();
        this.isBefore = watchRequest.getIsBefore();
        this.isFinish = watchRequest.getIsFinish();
        this.isException = watchRequest.getIsException();
        this.isSuccess = watchRequest.getIsSuccess();
        if (!watchRequest.getIsBefore() && !watchRequest.getIsFinish() && !watchRequest.getIsException() && !watchRequest.getIsSuccess()) {
            this.isFinish = true;
        }
        if (watchRequest.getExpand() <= 0) {
            this.expand = 1;
        } else if (watchRequest.getExpand() > MAX_EXPAND){
            this.expand = MAX_EXPAND;
        } else {
            this.expand = watchRequest.getExpand();
        }
        if (watchRequest.getSizeLimit() == 0) {
            this.sizeLimit = 10 * 1024 * 1024;
        } else {
            this.sizeLimit = watchRequest.getSizeLimit();
        }
        this.isRegEx = watchRequest.getIsRegEx();
        if (watchRequest.getNumberOfLimit() == 0) {
            this.numberOfLimit = 100;
        } else {
            this.numberOfLimit = watchRequest.getNumberOfLimit();
        }
        if(watchRequest.getExcludeClassPattern().equals("")){
            this.excludeClassPattern = null;
        }else {
            this.excludeClassPattern = watchRequest.getExcludeClassPattern();
        }
        this.listenerId = watchRequest.getListenerId();
        this.verbose = watchRequest.getVerbose();
        if(watchRequest.getMaxNumOfMatchedClass() == 0){
            this.maxNumOfMatchedClass = 50;
        }else {
            this.maxNumOfMatchedClass = watchRequest.getMaxNumOfMatchedClass();
        }
        this.jobId = watchRequest.getJobId();
    }



    public String getClassPattern() {
        return classPattern;
    }

    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    public String getExpress() {
        return express;
    }

    public void setExpress(String express) {
        this.express = express;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public void setBefore(boolean before) {
        isBefore = before;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    public boolean isException() {
        return isException;
    }

    public void setException(boolean exception) {
        isException = exception;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public Integer getExpand() {
        return expand;
    }

    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    public String getExcludeClassPattern() {
        return excludeClassPattern;
    }

    public void setExcludeClassPattern(String excludeClassPattern) {
        this.excludeClassPattern = excludeClassPattern;
    }

    public void setClassNameMatcher(Matcher classNameMatcher) {
        this.classNameMatcher = classNameMatcher;
    }

    public void setClassNameExcludeMatcher(Matcher classNameExcludeMatcher) {
        this.classNameExcludeMatcher = classNameExcludeMatcher;
    }

    public void setMethodNameMatcher(Matcher methodNameMatcher) {
        this.methodNameMatcher = methodNameMatcher;
    }

    public long getListenerId() {
        return listenerId;
    }

    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getMaxNumOfMatchedClass() {
        return maxNumOfMatchedClass;
    }

    public void setMaxNumOfMatchedClass(int maxNumOfMatchedClass) {
        this.maxNumOfMatchedClass = maxNumOfMatchedClass;
    }

    public long getJobId() {
        return jobId;
    }
}
