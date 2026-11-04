package com.alibaba.arthas.tunnel.server.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author hengyunabc 2021-07-12
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { ArthasTunnelApplication.class })
public class ArthasTunnelApplicationTest {

    @Test
    public void contextLoads() {
        System.out.println("hello");
    }

}