package com.taobao.arthas.service.impl;/**
 * @author: 風楪
 * @date: 2024/6/30 下午11:43
 */

import com.taobao.arthas.service.ArthasSampleService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: FengYe
 * @date: 2024/6/30 下午11:43
 * @description: ArthasSampleServiceImpl
 */
@Slf4j
public class ArthasSampleServiceImpl implements ArthasSampleService {
    @Override
    public String trace(String command) {
        log.info("receive command: {}", command);
        return "receive command: " + command;
    }
}