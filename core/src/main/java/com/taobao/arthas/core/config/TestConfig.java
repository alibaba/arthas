package com.taobao.arthas.core.config;


@Config
public class TestConfig {

    @NestedConfig
    SecondConfig secondConfig;
}
