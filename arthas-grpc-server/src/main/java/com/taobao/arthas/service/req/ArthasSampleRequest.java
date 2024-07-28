package com.taobao.arthas.service.req;/**
 * @author: 風楪
 * @date: 2024/7/14 上午4:28
 */

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.google.protobuf.*;
import com.taobao.arthas.protobuf.annotation.ProtobufCustomizedField;
import com.taobao.arthas.protobuf.annotation.ProtobufIgnore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/7/14 上午4:28
 * @description: ArthasSampleRequest
 */
@ProtobufClass
public class ArthasSampleRequest{

    private String name;
    private double age;
    private long price;
}
