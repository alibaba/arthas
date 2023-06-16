package com.alibaba.arthas.tunnel.common.util;

import io.grpc.Context;

/**
 * @author qiyue.zhang@aloudata.com
 * @description GrpcContextUtils
 * @date 2023/6/16 14:52
 */
public class GrpcContextUtils {
    
    private Context.Key<String> CONTEXT_CONN_ID = Context.key("conn_id");
}
