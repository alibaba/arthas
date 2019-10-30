package com.taobao.arthas.core.command.klass100.test.aa;

import com.alibaba.fastjson.JSON;

/**
 * @author bucong
 * @date 2019/10/18
 */
public class T1 {

    public static void main(String[] args) {
        System.out.println(JSON.parseObject("{'a':111L, 'm':true}"));
        System.out.println(">>>>>>>>>>>>>>>> main <<<<<<<<<<<<<");
    }
}
