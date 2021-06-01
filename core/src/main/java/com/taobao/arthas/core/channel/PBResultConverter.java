package com.taobao.arthas.core.channel;

import com.alibaba.arthas.channel.proto.SessionResult;
import com.alibaba.arthas.channel.proto.StatusResult;
import com.alibaba.arthas.channel.proto.SystemEnvResult;
import com.alibaba.arthas.channel.proto.UnknownResult;
import com.alibaba.arthas.channel.proto.VersionResult;
import com.google.protobuf.Any;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.SessionModel;
import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.command.model.SystemEnvModel;
import com.taobao.arthas.core.command.model.VersionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/8/16
 */
public class PBResultConverter {

    public List<Any> convertResults(List<ResultModel> resultModels) {
        List<Any> results = new ArrayList<Any>(resultModels.size());
        for (int i = 0; i < resultModels.size(); i++) {
            ResultModel resultModel = resultModels.get(i);
            if (resultModel instanceof SystemEnvModel) {
                SystemEnvModel systemEnvModel = (SystemEnvModel) resultModel;
                results.add(Any.pack(SystemEnvResult.newBuilder()
                        .setType(systemEnvModel.getType())
                        .putAllEnv(systemEnvModel.getEnv()).build()));
            } else if (resultModel instanceof SessionModel) {
                SessionModel sessionModel = (SessionModel) resultModel;
                results.add(Any.pack(SessionResult.newBuilder()
                        .setType(sessionModel.getType())
                        .setJavaPid(sessionModel.getJavaPid())
                        .setSessionId(sessionModel.getSessionId())
                        .build()));
            } else if (resultModel instanceof VersionModel) {
                VersionModel versionModel = (VersionModel) resultModel;
                results.add(Any.pack(VersionResult.newBuilder()
                        .setType(versionModel.getType())
                        .setVersion(versionModel.getVersion())
                        .build()));

            } else if (resultModel instanceof StatusModel) {
                StatusModel statusModel = (StatusModel) resultModel;
                results.add(Any.pack(StatusResult.newBuilder()
                        .setType(statusModel.getType())
                        .setStatusCode(statusModel.getStatusCode())
                        .setMessage(statusModel.getMessage() != null ? statusModel.getMessage() : "")
                        .build()));
            } else if (resultModel instanceof MessageModel) {
                MessageModel messageModel = (MessageModel) resultModel;
                results.add(Any.pack(StatusResult.newBuilder()
                        .setType(messageModel.getType())
                        .setMessage(messageModel.getMessage() != null ? messageModel.getMessage() : "")
                        .build()));
            } else  {
                // not supported proto format
                results.add(Any.pack(UnknownResult.newBuilder()
                        .setType(resultModel.getType())
                        .setMessage("unsupported proto format")
                        .build()));
            }
        }
        return results;
    }
}
