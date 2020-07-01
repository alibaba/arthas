package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.CompositeResultDistributor;
import com.taobao.arthas.core.distribution.ResultDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 复合结果分发器，将消息同时分发给其包含的真实分发器
 *
 * @author gongdewei 2020/4/30
 */
public class CompositeResultDistributorImpl implements CompositeResultDistributor {

    private List<ResultDistributor> distributors = Collections.synchronizedList(new ArrayList<ResultDistributor>());

    public CompositeResultDistributorImpl() {
    }

    public CompositeResultDistributorImpl(ResultDistributor ... distributors) {
        for (ResultDistributor distributor : distributors) {
            this.addDistributor(distributor);
        }
    }

    @Override
    public void addDistributor(ResultDistributor distributor) {
        distributors.add(distributor);
    }

    @Override
    public void removeDistributor(ResultDistributor distributor) {
        distributors.remove(distributor);
    }

    @Override
    public void appendResult(ResultModel result) {
        for (ResultDistributor distributor : distributors) {
            distributor.appendResult(result);
        }
    }

    @Override
    public void close() {
        for (ResultDistributor distributor : distributors) {
            distributor.close();
        }
    }
}
