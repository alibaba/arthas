package com.alibaba.arthas.web.test;

import com.taobao.arthas.boot.Bootstrap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApplication.class)
public class WebClientDemo {

    @Autowired
    private WebClientController webClientController;

    @Test
    public void bootstarp() throws Exception {
        assert  webClientController !=null;
    }
}
